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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * The Donations screen: register a donor, record a donation, and see all donations.
 *
 * <p>A <b>View</b> in MVC: it only shows things and calls {@link DonationController}
 * (and {@link LookupController} for the donation type list). The outcome of every action
 * is shown in the status bar.
 *
 * <p>The "Notify donors on new donation" checkbox is the visible part of the Observer
 * pattern: ticking it attaches the donor notification observer, unticking it detaches it.
 */
public class DonationsPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DonationController donationController;
    private final LookupController lookupController;
    private final StatusBar statusBar;

    // --- donor form ---
    private final JTextField donorNameField = new JTextField(15);
    private final JTextField donorEmailField = new JTextField(15);
    private final JTextField donorPhoneField = new JTextField(15);
    private final JComboBox<ContactChannel> channelBox = new JComboBox<>(ContactChannel.values());

    // --- donation form ---
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
        super(new BorderLayout(0, 8));
        this.donationController = donationController;
        this.lookupController = lookupController;
        this.statusBar = statusBar;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top: the two forms side by side.
        JPanel forms = new JPanel(new GridLayout(1, 2, 12, 0));
        forms.add(buildDonorForm());
        forms.add(buildDonationForm());
        add(forms, BorderLayout.NORTH);

        // Centre: the donations table.
        tableModel = new DefaultTableModel(
                new String[]{"Receipt no.", "Date", "Donor", "Type", "Method", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Ticked when the screen first opens: donors are notified by default.
        notifyCheckBox.setSelected(donationController.isDonorNotificationsEnabled());
        notifyCheckBox.addActionListener(e -> {
            boolean enabled = notifyCheckBox.isSelected();
            donationController.setDonorNotifications(enabled);
            statusBar.showResult(Result.ok(enabled
                    ? "Donors will be notified about new donations"
                    : "Donor notifications are turned off"));
        });
        viewReceiptButton.addActionListener(e -> viewReceipt());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(notifyCheckBox, BorderLayout.WEST);
        bottom.add(viewReceiptButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        methodBox.addActionListener(e -> showFieldsForMethod());
        showFieldsForMethod();
    }

    /** The small form to register a donor. */
    private JPanel buildDonorForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Register donor"));
        addRow(form, 0, new JLabel("Name"), donorNameField);
        addRow(form, 1, new JLabel("Email"), donorEmailField);
        addRow(form, 2, new JLabel("Phone"), donorPhoneField);
        addRow(form, 3, new JLabel("Preferred channel"), channelBox);
        JButton registerButton = new JButton("Register donor");
        registerButton.addActionListener(e -> registerDonor());
        addRow(form, 4, new JLabel(), registerButton);
        return form;
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
        JButton recordButton = new JButton("Record donation");
        recordButton.addActionListener(e -> recordDonation());
        addRow(form, 8, new JLabel(), recordButton);
        return form;
    }

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

    /** Shows only the fields that the chosen donation method needs. */
    private void showFieldsForMethod() {
        DonationMethod method = (DonationMethod) methodBox.getSelectedItem();
        boolean cash = method == DonationMethod.CASH;
        boolean online = method == DonationMethod.ONLINE;
        boolean inKind = method == DonationMethod.IN_KIND;
        setVisible(cash || online, amountLabel, amountField);
        setVisible(online, referenceLabel, referenceField);
        setVisible(inKind, itemLabel, itemField, quantityLabel, quantityField, valueLabel, valueField);
        revalidate();
        repaint();
    }

    private void setVisible(boolean visible, JComponent... components) {
        for (JComponent component : components) {
            component.setVisible(visible);
        }
    }

    private void registerDonor() {
        ContactChannel channel = (ContactChannel) channelBox.getSelectedItem();
        Result result = donationController.registerDonor(donorNameField.getText(),
                donorEmailField.getText(), donorPhoneField.getText(),
                channel == null ? null : channel.name());
        statusBar.showResult(result);
        if (result.success()) {
            donorNameField.setText("");
            donorEmailField.setText("");
            donorPhoneField.setText("");
            refresh(); // the new donor appears in the donor combo box
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
                itemField.getText(), quantityField.getText(), valueField.getText());
        statusBar.showResult(result);
        if (result.success()) {
            amountField.setText("");
            referenceField.setText("");
            itemField.setText("");
            quantityField.setText("");
            valueField.setText("");
            refresh();
        }
    }

    /** Opens the selected donation's receipt (basic + thank-you + tax if applicable) in a pop-up. */
    private void viewReceipt() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentDonations.size()) {
            statusBar.showResult(Result.fail("Select a donation first"));
            return;
        }
        Donation donation = currentDonations.get(row);
        String receipt = donationController.getReceipt(donation.getId());
        JOptionPane.showMessageDialog(this, receipt, "Receipt " + donation.getReceiptNumber(),
                JOptionPane.PLAIN_MESSAGE);
    }

    /** Reloads the donors, the donation types and the table. */
    @Override
    public void refresh() {
        Donor selectedDonor = (Donor) donorBox.getSelectedItem();
        DonationType selectedType = (DonationType) typeBox.getSelectedItem();

        donorBox.removeAllItems();
        for (Donor donor : donationController.listDonors()) {
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
                    donation.getDonor().getName(),
                    donation.getDonationType().getName(),
                    donation.getMethod().getLabel(),
                    String.format(Locale.US, "EGP %,.2f", donation.getValue())});
        }
        notifyCheckBox.setSelected(donationController.isDonorNotificationsEnabled());
    }
}
