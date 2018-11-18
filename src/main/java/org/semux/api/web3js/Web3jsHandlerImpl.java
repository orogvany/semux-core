package org.semux.api.web3js;

public class Web3jsHandlerImpl implements Web3jsHandler {

    public static final String VERSION = "2.0";

    @Override
    public Web3jsResponse handleRequest(Web3jsRequest request) {

        Web3jsMethod method = Web3jsMethod.valueOf(request.getMethod());

        Web3jsResponse response = new Web3jsResponse();
        response.setId(request.getId());
        response.setJsonrpc(VERSION);
        response.setResult(method.getResult(request.getParams()));
        return response;
    }
}
