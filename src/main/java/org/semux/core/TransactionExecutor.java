/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.semux.core.Amount.neg;
import static org.semux.core.Amount.sub;
import static org.semux.core.Amount.sum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.ethereum.vm.LogInfo;
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
import org.semux.vm.client.SemuxInternalTransaction;
import org.semux.vm.client.SemuxRepository;
import org.semux.vm.client.SemuxTransaction;

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
            SemuxBlock block, Blockchain chain) {
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

            boolean isVMTransaction = type == TransactionType.CREATE || type == TransactionType.CALL;

            // check fee (call and create use gas instead)
            if (isVMTransaction) {
                // applying a very strict check to avoid mistakes
                boolean valid = fee.equals(Amount.ZERO)
                        && tx.getGas() >= 21_000 && tx.getGas() <= config.spec().maxBlockGasLimit()
                        && tx.getGasPrice().getNano() >= 1 && tx.getGasPrice().getNano() <= Integer.MAX_VALUE; // a
                                                                                                               // theoretical
                                                                                                               // limit
                if (!valid) {
                    result.setCode(Code.INVALID_FEE);
                    continue;
                }
            } else {
                if (fee.lt(config.spec().minTransactionFee())) {
                    result.setCode(Code.INVALID_FEE);
                    continue;
                }
            }

            // check data length
            if (data.length > config.spec().maxTransactionDataSize(type)) {
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
                if (value.lt(config.spec().minDelegateBurnAmount())) {
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
                // Note: the second parameter should be height = block number + 1; here we're
                // checking if the fork is enabled at the end of last block.
                if (!chain.isForkActivated(Fork.VIRTUAL_MACHINE, block.getNumber())) {
                    result.setCode(Code.INVALID_TYPE);
                    break;
                }

                // the VM transaction executor will check balance and gas cost.
                // do proper refunds afterwards.

                executeVmTransaction(result, tx, as, ds, block, gasUsedInBlock);
                break;
            default:
                // unsupported transaction type
                result.setCode(Code.INVALID_TYPE);
                break;
            }

            if (result.getCode().isAcceptable()) {
                if (!isVMTransaction) {
                    // CREATEs and CALLs manages the nonce inside the VM
                    as.increaseNonce(from);
                }

                if (isVMTransaction) {
                    gasUsedInBlock += result.getGasUsed();
                } else {
                    gasUsedInBlock += config.spec().nonVMTransactionGasCost();
                }
            }

            result.setBlockNumber(block.getNumber());
        }

        return results;
    }

    private void executeVmTransaction(TransactionResult result, Transaction tx, AccountState as, DelegateState ds,
            SemuxBlock block, long gasUsedInBlock) {
        SemuxTransaction transaction = new SemuxTransaction(tx);
        Repository repository = new SemuxRepository(as, ds);
        ProgramInvokeFactory invokeFactory = new ProgramInvokeFactoryImpl();

        org.ethereum.vm.client.TransactionExecutor executor = new org.ethereum.vm.client.TransactionExecutor(
                transaction, block, repository, blockStore,
                config.spec().vmSpec(), invokeFactory, gasUsedInBlock, false);

        TransactionReceipt summary = executor.run();

        if (summary == null) {
            result.setCode(Code.INVALID);
        } else {
            result.setCode(summary.isSuccess() ? Code.SUCCESS : Code.FAILURE);
            result.setReturnData(summary.getReturnData());
            for (LogInfo log : summary.getLogs()) {
                result.addLog(log);
            }

            result.setGas(tx.getGas(), tx.getGasPrice(), summary.getGasUsed());

            result.setBlockNumber(block.getNumber());
            result.setInternalTransactions(summary.getInternalTransactions()
                    .stream()
                    .map(SemuxInternalTransaction::new)
                    .collect(Collectors.toList()));
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
     * @param chain
     *            the blockchain instance
     * @return
     */
    public TransactionResult execute(Transaction tx, AccountState as, DelegateState ds, SemuxBlock block,
            Blockchain chain) {
        return execute(Collections.singletonList(tx), as, ds, block, chain).get(0);
    }
}
