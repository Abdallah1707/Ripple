package com.ripple.view;

import com.ripple.controller.DonationController;
import com.ripple.controller.LookupController;
import com.ripple.controller.Result;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * The Donations screen, in two tabs:
 * <ul>
 *   <li><b>Donations</b>: record a donation, see all donations and open a receipt;</li>
 *   <li><b>Donors</b>: add, update and delete donors (Add / Update / Delete / Clear).</li>
 * </ul>
 * Together they are the complete Donations module (see {@code docs/crud-matrix.md}).
 *
 * <p>A <b>View</b> in MVC: it only shows things and calls {@link DonationController}
 * (and {@link LookupController} for the donation type list). The outcome of every action
 * is shown in the status bar.
 *
 * <p>The "Notify donors on new donation" checkbox is the visible part of the Observer
 * pattern: ticking it registers the donor notification observer, unticking it removes it.
 */
public class DonationsPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DonationController donationController;
    private final LookupController lookupController;
    private final StatusBar statusBar;

    // --- Donors tab ---
    private final JTextField donorNameField = new JTextField(18);
    private final JTextField donorEmailField = new JTextField(18);
    private final JTextField donorPhoneField = new JTextField(14);
    private final JTextField donorSocialField = new JTextField(14);
    private final JComboBox<ContactChannel> channelBox = new JComboBox<>(ContactChannel.values());
    private final DefaultTableModel donorTableModel;
    private final JTable donorTable;
    /** The donors currently shown, in the same order as the donor table rows. */
    private List<Donor> donors = List.of();

    // --- Donations tab ---
    private final JComboBox<Donor> donorBox = new JComboBox<>();
    private final JComboBox<DonationType> typeBox = new JComboBox<>();
    private final JComboBox<DonationMethod> methodBox = new JComboBox<>(DonationMethod.values());
    private final JTextField amountField = new JTextField(12);
    private final JTextField referenceField = new JTextField(12);
    private final JTextField itemField = new JTextField(12);
    private final JTextField quantityField = new JTextField(12);
    private final JTextField valueField = new JTextField(12);
    private final JLabel amountLabel = new JLabel("Amount (EGP)");
    private final JLabel referenceLabel = new JLabel("Transaction reference");
    private final JLabel itemLabel = new JLabel("Item");
    private final JLabel quantityLabel = new JLabel("Quantity");
    private final JLabel valueLabel = new JLabel("Estimated value (EGP)");
    // Extras: each one ticked becomes a decorator around the donation.
    private final JCheckBox urgentCheckBox = new JCheckBox("Urgent need");
    private final JCheckBox anonymousCheckBox = new JCheckBox("Anonymous (hide the donor's name in lists)");
    private final JTextField honoreeField = new JTextField(12);

    private final JCheckBox notifyCheckBox = new JCheckBox("Notify donors on new donation");
    private final JButton viewReceiptButton = new JButton("View receipt");
    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Donation> currentDonations = List.of();

    /**
     * Builds the screen.
     *
     * @param donationController the donations controller
     * @param lookupController   gives the list of donation types
     * @param statusBar          where to show the outcome of each action
     */
    public DonationsPanel(DonationController donationController, LookupController lookupController,
                          StatusBar statusBar) {
        super(new BorderLayout());
        this.donationController = donationController;
        this.lookupController = lookupController;
        this.statusBar = statusBar;

        tableModel = new DefaultTableModel(
                new String[]{"Receipt no.", "Date", "Donor", "Type", "Method", "Value", "Extras"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        donorTableModel = new DefaultTableModel(new String[]{"Name", "Email", "Phone", "Social handle", "Preferred channel"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // editing happens in the form, not in the table
            }
        };
        donorTable = new JTable(donorTableModel);
        donorTable.setRowHeight(24);
        donorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        donorTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillDonorFormFromSelection();
            }
        });
        FormHelper.onDoubleClick(donorTable, donorNameField::requestFocusInWindow); // edit the row in the form
        FormHelper.onDoubleClick(table, this::viewReceipt);

        FormHelper.placeholder(donorNameField, "Full name");
        FormHelper.placeholder(donorEmailField, "name@example.com");
        FormHelper.placeholder(donorPhoneField, "01012345678");
        FormHelper.placeholder(donorSocialField, "@handle");
        FormHelper.placeholder(amountField, "e.g. 500");
        FormHelper.placeholder(itemField, "e.g. Blankets");
        FormHelper.placeholder(quantityField, "e.g. 20");
        FormHelper.placeholder(valueField, "e.g. 4000");
        FormHelper.placeholder(honoreeField, "optional, e.g. Hana Ali");
        honoreeField.setToolTipText("Fill in to make this donation a gift in honor of someone");
        urgentCheckBox.setToolTipText("Urgent donations are listed first");
        donorPhoneField.setToolTipText("Exactly 11 digits; required if the donor prefers SMS");
        donorEmailField.setToolTipText("Required if the donor prefers Email");
        donorSocialField.setToolTipText("Starts with @; required if the donor prefers Social");
        FormHelper.onEnter(this::onSaveDonor, donorNameField, donorEmailField, donorPhoneField, donorSocialField);
        FormHelper.onEnter(this::recordDonation, amountField, referenceField, itemField, quantityField, valueField,
                honoreeField);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Donations", buildDonationsTab());
        tabs.addTab("Donors", buildDonorsTab());
        add(tabs, BorderLayout.CENTER);

        methodBox.addActionListener(e -> showFieldsForMethod());
        showFieldsForMethod();
    }

    // ---------------- Donations tab ----------------

    private JPanel buildDonationsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        tab.add(buildDonationForm(), BorderLayout.NORTH);
        tab.add(FormHelper.tableWithSearch(table), BorderLayout.CENTER);

        // Ticked when the screen first opens: donors are notified by default.
        notifyCheckBox.setSelected(donationController.isDonorNotificationsEnabled());
        notifyCheckBox.addActionListener(e -> {
            boolean enabled = notifyCheckBox.isSelected();
            donationController.setDonorNotifications(enabled);
            statusBar.showResult(Result.ok(enabled
                    ? "Donors will be notified about new donations"
                    : "Donor notifications are turned off"));
        });
        viewReceiptButton.setToolTipText("Show the receipt of the selected donation (or double-click a row)");
        viewReceiptButton.addActionListener(e -> viewReceipt());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(notifyCheckBox, BorderLayout.WEST);
        bottom.add(viewReceiptButton, BorderLayout.EAST);
        tab.add(bottom, BorderLayout.SOUTH);
        return tab;
    }

    /** The form to record a donation. Only the fields for the chosen method are visible. */
    private JPanel buildDonationForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Record donation"));
        addRow(form, 0, new JLabel("Donor"), donorBox);
        addRow(form, 1, new JLabel("Donation type"), typeBox);
        addRow(form, 2, new JLabel("Method"), methodBox);
        addRow(form, 3, amountLabel, amountField);
        addRow(form, 4, referenceLabel, referenceField);
        addRow(form, 5, itemLabel, itemField);
        addRow(form, 6, quantityLabel, quantityField);
        addRow(form, 7, valueLabel, valueField);
        addRow(form, 8, new JLabel("In honor of"), honoreeField);
        addRow(form, 9, new JLabel("Extras"), urgentCheckBox);
        addRow(form, 10, new JLabel(), anonymousCheckBox);
        addRow(form, 11, new JLabel(), FormHelper.button("Record donation",
                "Save this donation; its receipt is created automatically", this::recordDonation));
        return form;
    }

    /** Shows only the fields that the chosen donation method needs. */
    private void showFieldsForMethod() {
        DonationMethod method = (DonationMethod) methodBox.getSelectedItem();
        boolean cash = method == DonationMethod.CASH;
        boolean online = method == DonationMethod.ONLINE;
        boolean inKind = method == DonationMethod.IN_KIND;
        boolean cheque = method == DonationMethod.CHEQUE;
        setVisible(cash || online || cheque, amountLabel, amountField);
        // Online and cheque both use the reference field; only its label changes.
        referenceLabel.setText(cheque ? "Cheque no." : "Transaction reference");
        FormHelper.placeholder(referenceField, cheque ? "e.g. 000123" : "e.g. TX-1001");
        setVisible(online || cheque, referenceLabel, referenceField);
        setVisible(inKind, itemLabel, itemField, quantityLabel, quantityField, valueLabel, valueField);
        revalidate();
        repaint();
    }

    private void setVisible(boolean visible, JComponent... components) {
        for (JComponent component : components) {
            component.setVisible(visible);
        }
    }

    private void recordDonation() {
        Donor donor = (Donor) donorBox.getSelectedItem();
        DonationType type = (DonationType) typeBox.getSelectedItem();
        DonationMethod method = (DonationMethod) methodBox.getSelectedItem();
        Result result = donationController.recordDonation(
                donor == null ? 0 : donor.getId(),
                type == null ? 0 : type.getId(),
                method == null ? null : method.name(),
                amountField.getText(), referenceField.getText(),
                itemField.getText(), quantityField.getText(), valueField.getText(),
                urgentCheckBox.isSelected(), honoreeField.getText(), anonymousCheckBox.isSelected());
        statusBar.showResult(result);
        if (result.success()) {
            amountField.setText("");
            referenceField.setText("");
            itemField.setText("");
            quantityField.setText("");
            valueField.setText("");
            honoreeField.setText("");
            urgentCheckBox.setSelected(false);
            anonymousCheckBox.setSelected(false);
            refresh();
        }
    }

    /** Opens the selected donation's receipt (basic + thank-you + tax if applicable) in a pop-up. */
    private void viewReceipt() {
        int row = FormHelper.selectedModelRow(table); // correct even when sorted or filtered
        if (row < 0 || row >= currentDonations.size()) {
            statusBar.showResult(Result.fail("Select a donation first"));
            return;
        }
        Donation donation = currentDonations.get(row);
        String receipt = donationController.getReceipt(donation.getId());
        JOptionPane.showMessageDialog(this, receipt, "Receipt " + donation.getReceiptNumber(),
                JOptionPane.PLAIN_MESSAGE);
    }

    // ---------------- Donors tab ----------------

    private JPanel buildDonorsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        tab.add(FormHelper.tableWithSearch(donorTable), BorderLayout.CENTER);
        tab.add(buildDonorForm(), BorderLayout.SOUTH);
        return tab;
    }

    /** The form to add or edit a donor, with Add / Update / Delete / Clear. */
    private JPanel buildDonorForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Add or edit a donor"));
        addRow(form, 0, new JLabel("Name"), donorNameField);
        addRow(form, 1, new JLabel("Email"), donorEmailField);
        addRow(form, 2, new JLabel("Phone"), donorPhoneField);
        addRow(form, 3, new JLabel("Social handle"), donorSocialField);
        addRow(form, 4, new JLabel("Preferred channel"), channelBox);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(FormHelper.button("Add", "Save the form as a new donor", this::onAddDonor));
        buttons.add(FormHelper.button("Update", "Save the form over the selected donor", this::onUpdateDonor));
        buttons.add(FormHelper.button("Delete", "Delete the selected donor (not possible once they have donations)",
                this::onDeleteDonor));
        buttons.add(FormHelper.button("Clear", "Empty the form and unselect the row", this::clearDonorForm));
        addRow(form, 5, new JLabel(), buttons);
        return form;
    }

    /** Enter in the donor form: update the selected donor, or add a new one if none is selected. */
    private void onSaveDonor() {
        if (selectedDonor() == null) {
            onAddDonor();
        } else {
            onUpdateDonor();
        }
    }

    private void onAddDonor() {
        showDonorResult(donationController.saveDonor(null, donorNameField.getText(), donorEmailField.getText(),
                donorPhoneField.getText(), donorSocialField.getText(), selectedChannelName()));
    }

    private void onUpdateDonor() {
        Donor selected = selectedDonor();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a donor to update first."));
            return;
        }
        showDonorResult(donationController.saveDonor(selected.getId(), donorNameField.getText(),
                donorEmailField.getText(), donorPhoneField.getText(), donorSocialField.getText(),
                selectedChannelName()));
    }

    private void onDeleteDonor() {
        Donor selected = selectedDonor();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a donor to delete first."));
            return;
        }
        if (FormHelper.confirmDelete(this, "Delete donor '" + selected.getName() + "'?")) {
            showDonorResult(donationController.deleteDonor(selected.getId()));
        }
    }

    /** Shows the result; on success clears the form and reloads, so the donor lists match the database. */
    private void showDonorResult(Result result) {
        statusBar.showResult(result);
        if (result.success()) {
            clearDonorForm();
            refresh(); // the donor table and the donor combo box on the Donations tab
        }
    }

    private String selectedChannelName() {
        ContactChannel channel = (ContactChannel) channelBox.getSelectedItem();
        return channel == null ? null : channel.name();
    }

    private Donor selectedDonor() {
        int row = FormHelper.selectedModelRow(donorTable); // correct even when sorted or filtered
        return row < 0 || row >= donors.size() ? null : donors.get(row);
    }

    private void fillDonorFormFromSelection() {
        Donor selected = selectedDonor();
        if (selected == null) {
            return;
        }
        donorNameField.setText(selected.getName());
        donorEmailField.setText(selected.getEmail());
        donorPhoneField.setText(selected.getPhone());
        donorSocialField.setText(selected.getSocialHandle());
        channelBox.setSelectedItem(selected.getPreferredChannel());
    }

    private void clearDonorForm() {
        donorTable.clearSelection();
        donorNameField.setText("");
        donorEmailField.setText("");
        donorPhoneField.setText("");
        donorSocialField.setText("");
        channelBox.setSelectedIndex(0);
    }

    // ---------------- shared ----------------

    /** Adds one "label + field" row to a form that uses GridBagLayout. */
    private void addRow(JPanel form, int row, JLabel label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        form.add(label, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(field, c);
    }

    /** Reloads the donors (table and combo box), the donation types and the donations table. */
    @Override
    public void refresh() {
        Donor selectedDonor = (Donor) donorBox.getSelectedItem();
        DonationType selectedType = (DonationType) typeBox.getSelectedItem();

        donors = donationController.listDonors();
        donorTableModel.setRowCount(0);
        donorBox.removeAllItems();
        for (Donor donor : donors) {
            donorTableModel.addRow(new Object[]{donor.getName(), donor.getEmail(), donor.getPhone(),
                    donor.getSocialHandle(), donor.getPreferredChannel()});
            donorBox.addItem(donor);
            if (selectedDonor != null && donor.getId() == selectedDonor.getId()) {
                donorBox.setSelectedItem(donor); // keep the previous choice
            }
        }
        typeBox.removeAllItems();
        for (DonationType type : lookupController.listDonationTypes()) {
            typeBox.addItem(type);
            if (selectedType != null && type.getId() == selectedType.getId()) {
                typeBox.setSelectedItem(type);
            }
        }

        tableModel.setRowCount(0);
        currentDonations = donationController.listDonations();
        for (Donation donation : currentDonations) {
            tableModel.addRow(new Object[]{
                    donation.getReceiptNumber(),
                    donation.getCreatedAt().format(DATE_FORMAT),
                    donation.getDonorDisplayName(), // "Anonymous donor" for anonymous donations
                    donation.getDonationType().getName(),
                    donation.getMethod().getLabel(),
                    String.format(Locale.US, "EGP %,.2f", donation.getValue()),
                    String.join(", ", donation.getOptions())});
        }
        notifyCheckBox.setSelected(donationController.isDonorNotificationsEnabled());
    }
}
