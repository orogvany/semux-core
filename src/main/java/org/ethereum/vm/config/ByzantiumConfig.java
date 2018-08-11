/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.vm.config;

import java.math.BigInteger;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.GasCost;
import org.ethereum.vm.OpCode;
import org.ethereum.vm.program.exception.OutOfGasException;

public class ByzantiumConfig implements VMConfig {

    private static class GasCostByzantium extends GasCost {
        public int getBALANCE() {
            return 400;
        }

        public int getEXT_CODE_SIZE() {
            return 700;
        }

        public int getEXT_CODE_COPY() {
            return 700;
        }

        public int getSLOAD() {
            return 200;
        }

        public int getCALL() {
            return 700;
        }

        public int getSUICIDE() {
            return 5000;
        }

        public int getNEW_ACCT_SUICIDE() {
            return 25000;
        }

        public int getEXP_BYTE_GAS() {
            return 50;
        }
    }

    private static final GasCost NEW_GAS_COST = new GasCostByzantium();

    public ByzantiumConfig() {

    }

    private static DataWord maxAllowed(DataWord available) {
        return new DataWord(available.value().subtract(available.value().divide(BigInteger.valueOf(64))));
    }

    public DataWord getCallGas(OpCode op, DataWord requestedGas, DataWord availableGas) throws OutOfGasException {
        DataWord maxAllowed = maxAllowed(availableGas);
        return requestedGas.compareTo(maxAllowed) > 0 ? maxAllowed : requestedGas;
    }

    public DataWord getCreateGas(DataWord availableGas) {
        return maxAllowed(availableGas);
    }

    public long getTransactionCost() {
        return 21_000;
    }

    public GasCost getGasCost() {
        return NEW_GAS_COST;
    }
}