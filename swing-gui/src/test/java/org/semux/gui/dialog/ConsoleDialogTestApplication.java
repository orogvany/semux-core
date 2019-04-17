/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import org.semux.KernelMock;
import org.semux.gui.BaseTestApplication;
import org.semux.gui.SemuxGui;
import org.semux.gui.model.WalletModel;

/**
 */
public class ConsoleDialogTestApplication extends BaseTestApplication {

    private static final long serialVersionUID = 1L;

    SemuxGui gui;

    ConsoleDialog consoleDialog;

    ConsoleDialogTestApplication(WalletModel walletModel, KernelMock kernelMock) {
        super();
        gui = new SemuxGui(walletModel, kernelMock);
        consoleDialog = new ConsoleDialog(gui, this);
        consoleDialog.setVisible(true);
    }
}
