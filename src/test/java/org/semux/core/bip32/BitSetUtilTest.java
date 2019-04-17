/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.bip32;

import org.junit.Assert;
import org.junit.Test;
import org.semux.core.bip32.crypto.BitSetUtil;

import java.util.BitSet;
import java.util.Random;

public class BitSetUtilTest {

    @Test
    public void testConversion() {
        int numBytes = 10;
        byte[] bytes = new byte[numBytes];
        new Random().nextBytes(bytes);

        BitSet bitset = BitSetUtil.createBitset(bytes);
        byte[] converted = BitSetUtil.createBytes(bitset, numBytes);

        Assert.assertArrayEquals(bytes, converted);
    }
}
