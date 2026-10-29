package com.ripple.view;

import com.ripple.controller.LookupController;
import com.ripple.controller.LookupKind;
import com.ripple.controller.Result;
import com.ripple.model.DonationType;
import com.ripple.model.LookupEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * One lookup table on screen: a table of entries, a small form and
 * Add / Update / Delete / Clear buttons.
 *
 * <p>The same class is used twice (once per {@link LookupKind}); the tax-deductible column
 * and checkbox only appear for donation types. Like every view, it only calls the
 * controller and shows the {@link Result} it gets back.
 */
public class LookupTablePanel extends JPanel implements Refreshable {

    private final LookupController controller;
    private final LookupKind kind;
    private final StatusBar statusBar;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameField = new JTextField(20);
    private final JTextField descriptionField = new JTextField(30);
    private final JCheckBox taxCheckBox = new JCheckBox("Tax deductible");
    /** The entries currently shown, in the same order as the table rows. */
    private List<LookupEntry> entries = new ArrayList<>();

    /**
     * Builds the panel.
     *
     * @param controller the lookup controller
     * @param kind       which lookup table this panel shows
     * @param statusBar  where to show the outcome of each action
     */
    public LookupTablePanel(LookupController controller, LookupKind kind, StatusBar statusBar) {
        super(new BorderLayout(0, 8));
        this.controller = controller;
        this.kind = kind;
        this.statusBar = statusBar;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] columns = kind.hasTaxFlag()
                ? new String[]{"Name", "Description", "Tax deductible"}
                : new String[]{"Name", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // editing happens in the form, not in the table
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFormFromSelection();
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        entries = controller.list(kind);
        tableModel.setRowCount(0);
        for (LookupEntry entry : entries) {
            if (entry instanceof DonationType type) {
                tableModel.addRow(new Object[]{type.getName(), type.getDescription(),
                        type.isTaxDeductible() ? "Yes" : "No"});
            } else {
                tableModel.addRow(new Object[]{entry.getName(), entry.getDescription()});
            }
        }
        clearForm();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Add or edit"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("Name:"), c);
        c.gridx = 1;
        form.add(nameField, c);
        c.gridx = 2;
        form.add(new JLabel("Description:"), c);
        c.gridx = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(descriptionField, c);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        if (kind.hasTaxFlag()) {
            c.gridx = 4;
            form.add(taxCheckBox, c);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(button("Add", this::onAdd));
        buttons.add(button("Update", this::onUpdate));
        buttons.add(button("Delete", this::onDelete));
        buttons.add(button("Clear", this::clearForm));
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 5;
        form.add(buttons, c);
        return form;
    }

    private static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    private void onAdd() {
        Result result = controller.save(kind, null, nameField.getText(), descriptionField.getText(),
                taxCheckBox.isSelected());
        showAndRefresh(result);
    }

    private void onUpdate() {
        LookupEntry selected = selectedEntry();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a row to update first."));
            return;
        }
        Result result = controller.save(kind, selected.getId(), nameField.getText(), descriptionField.getText(),
                taxCheckBox.isSelected());
        showAndRefresh(result);
    }

    private void onDelete() {
        LookupEntry selected = selectedEntry();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a row to delete first."));
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete '" + selected.getName() + "'?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            showAndRefresh(controller.delete(kind, selected.getId()));
        }
    }

    /** Shows the result; on success reloads the table so it matches the database. */
    private void showAndRefresh(Result result) {
        statusBar.showResult(result);
        if (result.success()) {
            refresh();
        }
    }

    private LookupEntry selectedEntry() {
        int row = table.getSelectedRow();
        return row < 0 ? null : entries.get(row);
    }

    private void fillFormFromSelection() {
        LookupEntry selected = selectedEntry();
        if (selected == null) {
            return;
        }
        nameField.setText(selected.getName());
        descriptionField.setText(selected.getDescription());
        taxCheckBox.setSelected(selected instanceof DonationType type && type.isTaxDeductible());
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        descriptionField.setText("");
        taxCheckBox.setSelected(false);
    }
}
