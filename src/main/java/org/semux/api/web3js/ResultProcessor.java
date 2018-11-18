package org.semux.api.web3js;

import java.util.Map;

public interface ResultProcessor {
    String getResult(Map<String, String> request);
}
