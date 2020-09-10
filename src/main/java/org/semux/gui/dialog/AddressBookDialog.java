/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.semux.core.Wallet;
import org.semux.crypto.Hex;
import org.semux.gui.Action;
import org.semux.gui.AddressBookEntry;
import org.semux.gui.SemuxGui;
import org.semux.gui.SwingUtil;
import org.semux.message.GuiMessages;
import org.semux.util.ByteArray;
import org.semux.util.exception.UnreachableException;

public class AddressBookDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String[] columnNames = { GuiMessages.get("Name"), GuiMessages.get("Address") };

    private final transient Wallet wallet;
    private final transient SemuxGui gui;

    private final JTable table;
    private final AddressTableModel tableModel;

    public AddressBookDialog(JFrame parent, Wallet wallet, SemuxGui gui) {
        super(null, GuiMessages.get("AddressBook"), ModalityType.MODELESS);
        this.setName("AddressBookDialog");

        this.wallet = wallet;
        this.gui = gui;
        this.gui.getModel().addLockable(this);

        tableModel = new AddressTableModel();
        table = new JTable(tableModel);
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setRowHeight(25);
        table.getTableHeader().setPreferredSize(new Dimension(10000, 24));
        SwingUtil.setColumnWidths(table, 800, 0.25, 0.75);
        SwingUtil.setColumnAlignments(table, false, false);

        // auto sort
        table.setAutoCreateRowSorter(true);

        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.SOUTH);
        JButton btnNew = SwingUtil.createDefaultButton(GuiMessages.get("Add"), this, Action.ADD_ADDRESS);
        panel.add(btnNew);
        JButton btnEdit = SwingUtil.createDefaultButton(GuiMessages.get("Edit"), this, Action.EDIT_ADDRESS);
        panel.add(btnEdit);
        JButton btnCopy = SwingUtil.createDefaultButton(GuiMessages.get("Copy"), this, Action.COPY_ADDRESS);
        panel.add(btnCopy);
        JButton btnDelete = SwingUtil.createDefaultButton(GuiMessages.get("Delete"), this, Action.DELETE_ADDRESS);
        panel.add(btnDelete);

        JScrollPane scrollPane = new JScrollPane();
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(table);

        this.setTitle(GuiMessages.get("AddressBook"));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
        this.setModal(false);

        // display data
        refresh();
    }

    public Wallet getWallet() {
        return wallet;
    }

    private static class AddressTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private transient List<AddressBookEntry> addresses;

        AddressTableModel() {
            this.addresses = Collections.emptyList();
        }

        void setData(List<AddressBookEntry> addresses) {
            this.addresses = addresses;
            this.fireTableDataChanged();
        }

        AddressBookEntry getRow(int row) {
            if ((row >= 0) && (row < addresses.size())) {
                return addresses.get(row);
            }

            return null;
        }

        @Override
        public int getRowCount() {
            return addresses.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            AddressBookEntry entry = addresses.get(row);

            switch (column) {
            case 0:
                return entry.getName();
            case 1:
                return entry.getAddress();
            default:
                return null;
            }
        }
    }

    private AddressBookEntry getSelectedEntry() {
        int row = table.getSelectedRow();
        return (row != -1) ? tableModel.getRow(table.convertRowIndexToModel(row)) : null;
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Action action = Action.valueOf(e.getActionCommand());

        switch (action) {
        case REFRESH:
            refresh();
            break;
        case ADD_ADDRESS: {
            AddressBookUpdateDialog dialog = new AddressBookUpdateDialog(this, null, wallet, gui);
            dialog.setVisible(true);
            break;
        }
        case EDIT_ADDRESS: {
            AddressBookEntry entry = getSelectedEntry();
            if (entry == null) {
                JOptionPane.showMessageDialog(this, GuiMessages.get("SelectAddress"));
                break;
            }
            AddressBookUpdateDialog dialog = new AddressBookUpdateDialog(this, entry, wallet, gui);
            dialog.setVisible(true);
            break;
        }
        case COPY_ADDRESS:
        case DELETE_ADDRESS:
            AddressBookEntry entry = getSelectedEntry();
            if (entry != null) {
                if (action == Action.COPY_ADDRESS) {
                    Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                    cb.setContents(new StringSelection(entry.getAddress()), null);

                    JOptionPane.showMessageDialog(this, GuiMessages.get("AddressCopied", entry.getAddress()));
                } else {
                    wallet.removeAddressAlias(Hex.decode0x(entry.getAddress()));
                    wallet.flush();

                    gui.updateModel();
                }
            } else {
                JOptionPane.showMessageDialog(this, GuiMessages.get("SelectAddress"));
            }
            break;
        case LOCK:
            this.dispose();
            break;
        default:
            throw new UnreachableException();
        }
    }

    public void refresh() {
        List<AddressBookEntry> list = getAddressBookEntries();

        /*
         * update table model
         */
        AddressBookEntry e = getSelectedEntry();
        tableModel.setData(list);

        if (e != null) {
            for (int i = 0; i < list.size(); i++) {
                if (e.getName().equals(list.get(i).getName())) {
                    table.setRowSelectionInterval(table.convertRowIndexToView(i), table.convertRowIndexToView(i));
                    break;
                }
            }
        }
    }

    protected List<AddressBookEntry> getAddressBookEntries() {
        List<AddressBookEntry> entries = new ArrayList<>();

        if (wallet.isUnlocked()) {
            for (Map.Entry<ByteArray, String> address : wallet.getAddressAliases().entrySet()) {
                entries.add(new AddressBookEntry(address.getValue(), Hex.encode0x(address.getKey().getData())));
            }
        }

        entries.sort(AddressBookEntry::compareTo);

        return entries;
    }
}