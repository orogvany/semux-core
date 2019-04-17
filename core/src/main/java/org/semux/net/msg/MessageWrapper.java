/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import org.semux.util.TimeUtil;

/**
 * Utility that keeps track of the number of retries and lastTimestamp.
 *
 */
public class MessageWrapper {

    private final Message message;
    private long lastTimestamp = 0;

    private int retries = 0;
    private boolean isAnswered = false;

    /**
     * Create a message round trip.
     * 
     * @param message
     */
    public MessageWrapper(Message message) {
        this.message = message;
        saveTime();
    }

    public void saveTime() {
        lastTimestamp = TimeUtil.currentTimeMillis();
    }

    public void answer() {
        this.isAnswered = true;
    }

    public void increaseRetries() {
        ++retries;
    }

    public Message getMessage() {
        return message;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public int getRetries() {
        return retries;
    }

    public boolean isAnswered() {
        return isAnswered;
    }
}