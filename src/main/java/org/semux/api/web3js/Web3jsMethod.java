package org.semux.api.web3js;

import java.util.Map;

public enum Web3jsMethod {
    web3_clientVersion,
    web3_sha3,
    net_version,
    net_listening,
    net_peerCount,
    eth_protocolVersion,
    eth_syncing,
    eth_coinbase,
    eth_mining,
    eth_hashrate,
    eth_gasPrice,
    eth_accounts,
    eth_blockNumber,
    eth_getBalance,
    eth_getStorageAt,
    eth_getTransactionCount,
    eth_getBlockTransactionCountByNumber,
    eth_getUncleCountByBlockHash,
    eth_getUncleCountByBlockNumber,
    eth_getCode,
    eth_sign,
    eth_sendTransaction,
    eth_sendRawTransaction,
    eth_call,
    eth_getBlockByHash,
    eth_getBlockByNumber,
    eth_getTransactionByHash,
    eth_getTransactionByBlockHashAndIndex,
    eth_getTransactionByBlockNumberAndIndex,
    eth_getTransactionReceipt,
    eth_getUncleByBlockHashAndIndex,
    eth_getUncleByBlockNumberAndIndex,
    eth_newFilter,
    eth_newBlockFilter,
    eth_newPendingTransactionFilter,
    eth_uninstallFilter,
    eth_getFilterChanges,
    eth_getFilterLogs,
    eth_getLogs,
    eth_getWork,
    eth_submitWork,
    eth_submitHashrate,
    eth_getProof,
    db_putString,
    db_getString,
    db_putHex,
    db_getHex,
    shh_version,
    shh_post,
    shh_newIdentity,
    shh_hasIdentity,
    shh_newGroup,
    shh_addToGroup,
    shh_newFilter,
    shh_uninstallFilter,
    shh_getFilterChanges,
    shh_getMessages;

    private ResultProcessor processor;

    public String getResult(Map<String, String> request) {
        if (processor != null) {
            return processor.getResult(request);
        }
        return null;
    }
}
