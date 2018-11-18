package org.semux.api.web3js.calls;

import org.semux.api.web3js.ResultProcessor;
import org.semux.api.web3js.Web3jsHandler;
import org.semux.api.web3js.Web3jsRequest;
import org.semux.api.web3js.Web3jsResponse;

import java.util.Map;

public class Call implements ResultProcessor {

    @Override
    public String getResult(Map<String, String> request) {

        String from = request.get("from");
        String to = request.get("to");
        String gas = request.get("gas");
        String gasPrice = request.get("gasPrice");
        String value = request.get("value");
        String data = request.get("data");

        return null;
    }
}
