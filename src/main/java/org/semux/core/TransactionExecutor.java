/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.ethereum.vm.LogInfo;
import org.ethereum.vm.chainspec.Spec;
import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.client.TransactionReceipt;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.semux.config.Config;
import org.semux.core.TransactionResult.Code;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.util.Bytes;
import org.semux.vm.client.SemuxBlock;
import org.semux.vm.client.SemuxRepository;
import org.semux.vm.client.SemuxTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.semux.core.Amount.*;

/**
 * Transaction executor
 */
public class TransactionExecutor {

    private static final boolean[] valid = new boolean[256];
    static {
        for (byte b : Bytes.of("abcdefghijklmnopqrstuvwxyz0123456789_")) {
            valid[b & 0xff] = true;
        }
    }

    private BlockStore blockStore;

    /**
     * Validate delegate name.
     *
     * @param data
     */
    public static boolean validateDelegateName(byte[] data) {
        if (data.length < 3 || data.length > 16) {
            return false;
        }

        for (byte b : data) {
            if (!valid[b & 0xff]) {
                return false;
            }
        }

        return true;
    }

    private Config config;

    /**
     * Creates a new transaction executor.
     *
     * @param config
     */
    public TransactionExecutor(Config config, BlockStore blockStore) {
        this.config = config;
        this.blockStore = blockStore;
    }

    /**
     * Execute a list of transactions.
     *
     * NOTE: transaction format and signature are assumed to be success.
     *
     * @param txs
     *            transactions
     * @param as
     *            account state
     * @param ds
     *            delegate state
     * @return
     */
    public List<TransactionResult> execute(List<Transaction> txs, AccountState as, DelegateState ds,
            SemuxBlock block) {
        List<TransactionResult> results = new ArrayList<>();

        long gasUsedInBlock = 0;
        for (Transaction tx : txs) {
            TransactionResult result = new TransactionResult();
            results.add(result);

            TransactionType type = tx.getType();
            byte[] from = tx.getFrom();
            byte[] to = tx.getTo();
            Amount value = tx.getValue();
            long nonce = tx.getNonce();
            Amount fee = tx.getFee();
            byte[] data = tx.getData();

            Account acc = as.getAccount(from);
            Amount available = acc.getAvailable();
            Amount locked = acc.getLocked();

            // check nonce
            if (nonce != acc.getNonce()) {
                result.setCode(Code.INVALID_NONCE);
                continue;
            }

            // check fee
            if (fee.lt(config.minTransactionFee())) {
                result.setCode(Code.INVALID_FEE);
                continue;
            }

            // check data length
            if (data.length > config.maxTransactionDataSize(type)) {
                result.setCode(Code.INVALID_DATA);
                continue;
            }

            switch (type) {
            case TRANSFER: {
                if (fee.lte(available) && value.lte(available) && sum(value, fee).lte(available)) {
                    as.adjustAvailable(from, neg(sum(value, fee)));
                    as.adjustAvailable(to, value);
                } else {
                    result.setCode(Code.INSUFFICIENT_AVAILABLE);
                }
                break;
            }
            case DELEGATE: {
                if (!validateDelegateName(data)) {
                    result.setCode(Code.INVALID_DELEGATE_NAME);
                    break;
                }
                if (value.lt(config.minDelegateBurnAmount())) {
                    result.setCode(Code.INVALID_DELEGATE_BURN_AMOUNT);
                    break;
                }
                if (!Arrays.equals(Bytes.EMPTY_ADDRESS, to)) {
                    result.setCode(Code.INVALID_DELEGATE_BURN_ADDRESS);
                    break;
                }

                if (fee.lte(available) && value.lte(available) && sum(value, fee).lte(available)) {
                    if (ds.register(from, data)) {
                        as.adjustAvailable(from, neg(sum(value, fee)));
                    } else {
                        result.setCode(Code.INVALID_DELEGATING);
                    }
                } else {
                    result.setCode(Code.INSUFFICIENT_AVAILABLE);
                }
                break;
            }
            case VOTE: {
                if (fee.lte(available) && value.lte(available) && sum(value, fee).lte(available)) {
                    if (ds.vote(from, to, value)) {
                        as.adjustAvailable(from, neg(sum(value, fee)));
                        as.adjustLocked(from, value);
                    } else {
                        result.setCode(Code.INVALID_VOTING);
                    }
                } else {
                    result.setCode(Code.INSUFFICIENT_AVAILABLE);
                }
                break;
            }
            case UNVOTE: {
                if (available.lt(fee)) {
                    result.setCode(Code.INSUFFICIENT_AVAILABLE);
                    break;
                }
                if (locked.lt(value)) {
                    result.setCode(Code.INSUFFICIENT_LOCKED);
                    break;
                }

                if (ds.unvote(from, to, value)) {
                    as.adjustAvailable(from, sub(value, fee));
                    as.adjustLocked(from, neg(value));
                } else {
                    result.setCode(Code.INVALID_UNVOTING);
                }
                break;
            }

            case CALL:
            case CREATE:
                long maxGasFee = tx.getGas() * tx.getGasPrice();
                Amount maxCost = sum(sum(value, fee), Unit.NANO_SEM.of(maxGasFee));
                if (maxCost.lte(available)) {
                    // VM calls still use fees
                    as.adjustAvailable(from, neg(sum(value, fee)));

                    if (tx.getGas() > config.vmMaxBlockGasLimit()) {
                        result.setCode(Code.INVALID_GAS);
                    } else if (block == null) {
                        // workaround for pending manager so it doesn't execute these
                        // we charge gas later
                        as.increaseNonce(from);
                        result.setCode(Code.SUCCESS);
                    } else {
                        executeVmTransaction(result, tx, as, block, gasUsedInBlock);
                        gasUsedInBlock += result.getGasUsed();
                    }
                } else {
                    result.setCode(Code.INSUFFICIENT_AVAILABLE);
                }
                break;

            default:
                // unsupported transaction type
                result.setCode(Code.INVALID_TYPE);
                break;
            }

            // increase nonce if success
            // creates and calls increase their own nonces internal to VM
            if (result.getCode().isAccepted() && type != TransactionType.CREATE && type != TransactionType.CALL) {
                as.increaseNonce(from);
            }
        }

        return results;
    }

    private void executeVmTransaction(TransactionResult result, Transaction tx, AccountState as, SemuxBlock block,
            long gasUsedInBlock) {
        SemuxTransaction transaction = new SemuxTransaction(tx);
        Repository repository = new SemuxRepository(as);
        ProgramInvokeFactory invokeFactory = new ProgramInvokeFactoryImpl();

        org.ethereum.vm.client.TransactionExecutor executor = new org.ethereum.vm.client.TransactionExecutor(
                transaction, block, repository, blockStore,
                Spec.DEFAULT, invokeFactory, gasUsedInBlock, false);

        TransactionReceipt summary = executor.run();
        if (summary == null) {
            result.setCode(Code.FAILURE);
        } else {
            for (LogInfo log : summary.getLogs()) {
                result.addLog(log);
            }
            result.setGasUsed(summary.getGasUsed());
            result.setReturnData(summary.getReturnData());
            result.setCode(summary.isSuccess() ? Code.SUCCESS : Code.FAILURE);
        }
    }

    /**
     * Execute one transaction.
     *
     * NOTE: transaction format and signature are assumed to be success.
     *
     * @param as
     *            account state
     * @param ds
     *            delegate state
     * @return
     */
    public TransactionResult execute(Transaction tx, AccountState as, DelegateState ds, SemuxBlock block) {
        return execute(Collections.singletonList(tx), as, ds, block).get(0);
    }
}
