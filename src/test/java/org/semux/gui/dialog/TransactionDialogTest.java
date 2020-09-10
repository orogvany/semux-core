/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import static org.semux.core.TransactionType.TRANSFER;
import static org.semux.core.Unit.SEM;

import java.math.BigDecimal;
import java.time.Instant;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semux.core.Amount;
import org.semux.core.Transaction;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.gui.SwingUtil;
import org.semux.gui.model.WalletModel;
import org.semux.rules.KernelRule;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionDialogTest extends AssertJSwingJUnitTestCase {

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Mock
    WalletModel walletModel;

    @Test
    public void testDisplayTransferTransaction() {
        kernelRule1.getKernel().start();

        Key from = new Key();
        Key to = new Key();
        Amount value = Amount.of(1000, SEM);
        Amount fee = Amount.of(new BigDecimal("0.05"), SEM);
        long nonce = 0L;
        long now = Instant.now().toEpochMilli();
        byte[] data = "some data".getBytes();
        Transaction tx = new Transaction(kernelRule1.getKernel().getConfig().network(), TRANSFER, to.toAddress(), value,
                fee, nonce, now, data).sign(from);

        TransactionDialogTestApplication application = GuiActionRunner
                .execute(() -> new TransactionDialogTestApplication(walletModel, kernelRule1.getKernel(), tx));

        FrameFixture window = new FrameFixture(robot(), application);
        DialogFixture dialog = window.show().requireVisible().moveToFront()
                .dialog("TransactionDialog").requireVisible();

        dialog.textBox("hashText").requireVisible().requireText(Hex.encode0x(tx.getHash()));
        dialog.textBox("fromText").requireVisible().requireText(Hex.encode0x(from.toAddress()));
        dialog.textBox("toText").requireVisible().requireText(Hex.encode0x(to.toAddress()));
        dialog.label("valueText").requireVisible().requireText(SwingUtil.formatAmount(value));
        dialog.label("feeText").requireVisible().requireText(SwingUtil.formatAmount(fee));
        dialog.label("nonceText").requireVisible().requireText("0");
        dialog.label("timestampText").requireVisible().requireText(SwingUtil.formatTimestamp(now));
        dialog.textBox("dataText").requireVisible().requireText(Hex.encode0x(data));

        dialog.button("display").click();
    }

    @Override
    protected void onSetUp() {

    }
}
