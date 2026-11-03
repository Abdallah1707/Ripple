package com.ripple.view;

import com.ripple.controller.BeneficiaryController;
import com.ripple.controller.Result;
import com.ripple.model.Beneficiary;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
 * The Beneficiaries screen: a table of beneficiaries, a small form and
 * Add / Update / Delete / Clear buttons.
 *
 * <p>A <b>View</b> in MVC: it only shows things and calls {@link BeneficiaryController}.
 * The outcome of every action is shown in the status bar.
 */
public class BeneficiariesPanel extends JPanel implements Refreshable {

    private final BeneficiaryController controller;
    private final StatusBar statusBar;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField phoneField = new JTextField(14);
    private final JTextField addressField = new JTextField(30);
    private final JTextField needsField = new JTextField(30);
    /** The beneficiaries currently shown, in the same order as the table rows. */
    private List<Beneficiary> beneficiaries = new ArrayList<>();

    /**
     * Builds the screen.
     *
     * @param controller the beneficiary controller
     * @param statusBar  where to show the outcome of each action
     */
    public BeneficiariesPanel(BeneficiaryController controller, StatusBar statusBar) {
        super(new BorderLayout(0, 8));
        this.controller = controller;
        this.statusBar = statusBar;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        tableModel = new DefaultTableModel(new String[]{"Name", "Email", "Phone", "Address", "Needs"}, 0) {
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
        FormHelper.onDoubleClick(table, nameField::requestFocusInWindow); // edit the row in the form

        FormHelper.placeholder(nameField, "Full name");
        FormHelper.placeholder(emailField, "name@example.com");
        FormHelper.placeholder(phoneField, "01012345678");
        FormHelper.placeholder(addressField, "e.g. 12 Tahrir St, Cairo");
        FormHelper.placeholder(needsField, "e.g. Food, school supplies");
        phoneField.setToolTipText("Optional; 8-15 digits");
        FormHelper.onEnter(this::onSave, nameField, emailField, phoneField, addressField, needsField);

        add(FormHelper.tableWithSearch(table), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        beneficiaries = controller.listBeneficiaries();
        tableModel.setRowCount(0);
        for (Beneficiary b : beneficiaries) {
            tableModel.addRow(new Object[]{b.getName(), b.getEmail(), b.getPhone(), b.getAddress(), b.getNeeds()});
        }
        clearForm();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Add or edit a beneficiary"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        addRow(form, c, 0, "Name:", nameField, "Email:", emailField);
        addRow(form, c, 1, "Phone:", phoneField, "Address:", addressField);
        addRow(form, c, 2, "Needs:", needsField, null, null);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(FormHelper.button("Add", "Save the form as a new beneficiary", this::onAdd));
        buttons.add(FormHelper.button("Update", "Save the form over the selected beneficiary", this::onUpdate));
        buttons.add(FormHelper.button("Delete", "Delete the selected beneficiary", this::onDelete));
        buttons.add(FormHelper.button("Clear", "Empty the form and unselect the row", this::clearForm));
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 4;
        form.add(buttons, c);
        return form;
    }

    private static void addRow(JPanel form, GridBagConstraints c, int row, String label1, JTextField field1,
                               String label2, JTextField field2) {
        c.gridy = row;
        c.gridwidth = 1;
        c.gridx = 0;
        form.add(new JLabel(label1), c);
        c.gridx = 1;
        addStretched(form, c, field1);
        if (label2 != null) {
            c.gridx = 2;
            form.add(new JLabel(label2), c);
            c.gridx = 3;
            addStretched(form, c, field2);
        }
    }

    /**
     * Adds a field that grows with the form. Without the weight, a form narrower than its
     * preferred width would shrink every field to its minimum size.
     */
    private static void addStretched(JPanel form, GridBagConstraints c, JTextField field) {
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(field, c);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
    }

    /** Enter in the form: update the selected row, or add a new one if no row is selected. */
    private void onSave() {
        if (selectedBeneficiary() == null) {
            onAdd();
        } else {
            onUpdate();
        }
    }

    private void onAdd() {
        showAndRefresh(controller.saveBeneficiary(null, nameField.getText(), emailField.getText(),
                phoneField.getText(), addressField.getText(), needsField.getText()));
    }

    private void onUpdate() {
        Beneficiary selected = selectedBeneficiary();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a row to update first."));
            return;
        }
        showAndRefresh(controller.saveBeneficiary(selected.getId(), nameField.getText(), emailField.getText(),
                phoneField.getText(), addressField.getText(), needsField.getText()));
    }

    private void onDelete() {
        Beneficiary selected = selectedBeneficiary();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a row to delete first."));
            return;
        }
        if (FormHelper.confirmDelete(this, "Delete '" + selected.getName() + "'?")) {
            showAndRefresh(controller.deleteBeneficiary(selected.getId()));
        }
    }

    /** Shows the result; on success reloads the table so it matches the database. */
    private void showAndRefresh(Result result) {
        statusBar.showResult(result);
        if (result.success()) {
            refresh();
        }
    }

    private Beneficiary selectedBeneficiary() {
        int row = FormHelper.selectedModelRow(table); // correct even when sorted or filtered
        return row < 0 ? null : beneficiaries.get(row);
    }

    private void fillFormFromSelection() {
        Beneficiary selected = selectedBeneficiary();
        if (selected == null) {
            return;
        }
        nameField.setText(selected.getName());
        emailField.setText(selected.getEmail());
        phoneField.setText(selected.getPhone());
        addressField.setText(selected.getAddress());
        needsField.setText(selected.getNeeds());
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        addressField.setText("");
        needsField.setText("");
    }
}
