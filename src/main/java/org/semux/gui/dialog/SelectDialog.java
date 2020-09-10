/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.semux.gui.Action;
import org.semux.gui.SwingUtil;
import org.semux.message.GuiMessages;
import org.semux.util.exception.UnreachableException;

public class SelectDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final JComboBox<Object> comboBox;
    private int selected = -1;

    public SelectDialog(JFrame parent, String message, List<?> options) {
        super(parent, GuiMessages.get("Select"), ModalityType.TOOLKIT_MODAL);

        JLabel labelLogo = new JLabel("");
        labelLogo.setIcon(SwingUtil.loadImage("logo", 96, 96));

        JLabel lblMessage = new JLabel(message);

        JButton btnOk = SwingUtil.createDefaultButton(GuiMessages.get("OK"), this, Action.OK);
        btnOk.setSelected(true);

        JButton btnCancel = SwingUtil.createDefaultButton(GuiMessages.get("Cancel"), this, Action.CANCEL);

        comboBox = new JComboBox<>();
        comboBox.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        for (Object opt : options) {
            comboBox.addItem(opt);
        }
        comboBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    actionPerformed(new ActionEvent(SelectDialog.this, 0, Action.OK.name()));
                }
            }
        });

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(labelLogo)
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblMessage)
                        .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                            .addGap(101)
                            .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
                        .addComponent(comboBox, GroupLayout.PREFERRED_SIZE, 512, Short.MAX_VALUE))
                    .addGap(17))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addComponent(labelLogo))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(21)
                            .addComponent(lblMessage)
                            .addGap(18)
                            .addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGap(16)
                            .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(btnOk)
                                .addComponent(btnCancel))))
                    .addContainerGap(20, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case OK:
            selected = comboBox.getSelectedIndex();
            this.dispose();
            break;
        case CANCEL:
            selected = -1;
            this.dispose();
            break;
        default:
            throw new UnreachableException();
        }
    }

    /**
     * Shows this dialog and waits until it's done.
     * 
     * @return the input or null
     */
    public int showAndGet() {
        this.setVisible(true);
        return selected;
    }
}
