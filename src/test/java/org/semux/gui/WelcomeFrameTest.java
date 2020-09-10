/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.semux.Network;
import org.semux.core.Wallet;
import org.semux.crypto.Key;

public class WelcomeFrameTest extends AssertJSwingJUnitTestCase {

    private WelcomeFrame frame;

    private Wallet wallet;
    private FrameFixture window;

    @Override
    protected void onSetUp() {
        try {
            wallet = new Wallet(File.createTempFile("wallet1", ".data"), Network.DEVNET);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        frame = GuiActionRunner.execute(() -> new WelcomeFrame(wallet));

        // IMPORTANT: note the call to 'robot()'
        // we must use the Robot from AssertJSwingJUnitTestCase
        window = new FrameFixture(robot(), frame);
        window.show(); // shows the frame to test
    }

    @Override
    public void onTearDown() {
        try {
            wallet.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testBasics() {
        window.requireVisible();
    }

    @Test
    public void testCreate() {
        String password = "abc";
        window.textBox("txtPassword").requireEditable().setText(password);
        window.textBox("txtPasswordRepeat").requireEditable().setText(password);
        window.button("btnNext").click();

        await().until(() -> wallet.exists());
        assertTrue(wallet.isUnlocked());
        // wallet size is 0 until HD wallet initialized
        assertEquals(0, wallet.size());
        assertEquals(password, wallet.getPassword());
    }

    @Test
    public void testImport() throws IOException {
        // mock a wallet
        String password = "abc";
        Wallet w = new Wallet(File.createTempFile("wallet2", ".data"), Network.DEVNET);
        w.unlock(password);
        w.addAccount(new Key());
        w.flush();

        // FIXME: selectFile() is not working
        // window.radioButton("btnImport").requireVisible().click();
        // window.fileChooser().selectFile(w.getFile()).click().approve();

        GuiActionRunner.execute(() -> frame.selectRecover(w.getFile()));

        // enter password
        window.textBox("txtPassword").requireEditable().setText(password);

        // import
        window.button("btnNext").click();

        // assertions
        await().until(() -> wallet.exists());
        assertTrue(wallet.isUnlocked());
        assertEquals(1, wallet.size());
        assertEquals(password, wallet.getPassword());
        assertEquals(w.getAccount(0), wallet.getAccount(0));
    }
}
