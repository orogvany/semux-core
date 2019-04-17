/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.core.bip32.wallet.key;

public enum Curve {
    bitcoin("Bitcoin seed"), ed25519("ed25519 seed");

    private final String seed;

    Curve(String seed) {
        this.seed = seed;
    }

    public String getSeed() {
        return seed;
    }
}
