package com.ripple.view;

import com.ripple.controller.EventController;
import com.ripple.controller.Result;
import com.ripple.controller.VolunteerController;
import com.ripple.model.Event;
import com.ripple.model.Volunteer;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
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
 * The Volunteers screen: a table of volunteers, a small form with
 * Add / Update / Delete / Clear buttons, and an "Assign to event" option for the
 * selected volunteer.
 *
 * <p>A <b>View</b> in MVC: it only shows things and calls {@link VolunteerController}
 * (and {@link EventController} for the list of events). The outcome of every action is
 * shown in the status bar.
 */
public class VolunteersPanel extends JPanel implements Refreshable {

    private final VolunteerController volunteerController;
    private final EventController eventController;
    private final StatusBar statusBar;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField phoneField = new JTextField(14);
    private final JTextField skillsField = new JTextField(24);
    private final JTextField availabilityField = new JTextField(24);
    private final JComboBox<Event> eventBox = new JComboBox<>();
    /** The volunteers currently shown, in the same order as the table rows. */
    private List<Volunteer> volunteers = new ArrayList<>();

    /**
     * Builds the screen.
     *
     * @param volunteerController the volunteer controller
     * @param eventController     the event controller (fills the "assign to event" list)
     * @param statusBar           where to show the outcome of each action
     */
    public VolunteersPanel(VolunteerController volunteerController, EventController eventController,
                           StatusBar statusBar) {
        super(new BorderLayout(0, 8));
        this.volunteerController = volunteerController;
        this.eventController = eventController;
        this.statusBar = statusBar;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        tableModel = new DefaultTableModel(new String[]{"Name", "Email", "Phone", "Skills", "Availability"}, 0) {
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
        FormHelper.placeholder(skillsField, "e.g. First aid, driving");
        FormHelper.placeholder(availabilityField, "e.g. Weekends");
        phoneField.setToolTipText("Optional; 8-15 digits");
        FormHelper.onEnter(this::onSave, nameField, emailField, phoneField, skillsField, availabilityField);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.add(buildForm(), BorderLayout.CENTER);
        south.add(buildAssignPanel(), BorderLayout.SOUTH);
        add(FormHelper.tableWithSearch(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        volunteers = volunteerController.listVolunteers();
        tableModel.setRowCount(0);
        for (Volunteer v : volunteers) {
            tableModel.addRow(new Object[]{v.getName(), v.getEmail(), v.getPhone(), v.getSkills(),
                    v.getAvailability()});
        }
        eventBox.removeAllItems();
        for (Event event : eventController.listEvents()) {
            eventBox.addItem(event);
        }
        clearForm();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Add or edit a volunteer"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        addRow(form, c, 0, "Name:", nameField, "Email:", emailField);
        addRow(form, c, 1, "Phone:", phoneField, "Skills:", skillsField);
        addRow(form, c, 2, "Availability:", availabilityField, null, null);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(FormHelper.button("Add", "Save the form as a new volunteer", this::onAdd));
        buttons.add(FormHelper.button("Update", "Save the form over the selected volunteer", this::onUpdate));
        buttons.add(FormHelper.button("Delete", "Delete the selected volunteer", this::onDelete));
        buttons.add(FormHelper.button("Clear", "Empty the form and unselect the row", this::clearForm));
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 4;
        form.add(buttons, c);
        return form;
    }

    private JPanel buildAssignPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Assign the selected volunteer to an event"));
        panel.add(new JLabel("Event:"));
        panel.add(eventBox);
        panel.add(FormHelper.button("Assign to event", "Assign the selected volunteer to the chosen event",
                this::onAssign));
        return panel;
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
        if (selectedVolunteer() == null) {
            onAdd();
        } else {
            onUpdate();
        }
    }

    private void onAdd() {
        showAndRefresh(volunteerController.saveVolunteer(null, nameField.getText(), emailField.getText(),
                phoneField.getText(), skillsField.getText(), availabilityField.getText()));
    }

    private void onUpdate() {
        Volunteer selected = selectedVolunteer();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a row to update first."));
            return;
        }
        showAndRefresh(volunteerController.saveVolunteer(selected.getId(), nameField.getText(),
                emailField.getText(), phoneField.getText(), skillsField.getText(), availabilityField.getText()));
    }

    private void onDelete() {
        Volunteer selected = selectedVolunteer();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a row to delete first."));
            return;
        }
        if (FormHelper.confirmDelete(this,
                "Delete '" + selected.getName() + "'? Their event assignments are removed too.")) {
            showAndRefresh(volunteerController.deleteVolunteer(selected.getId()));
        }
    }

    private void onAssign() {
        Volunteer selected = selectedVolunteer();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a volunteer in the table first."));
            return;
        }
        Event event = (Event) eventBox.getSelectedItem();
        statusBar.showResult(volunteerController.assignToEvent(selected.getId(), event == null ? 0 : event.getId()));
    }

    /** Shows the result; on success reloads the table so it matches the database. */
    private void showAndRefresh(Result result) {
        statusBar.showResult(result);
        if (result.success()) {
            refresh();
        }
    }

    private Volunteer selectedVolunteer() {
        int row = FormHelper.selectedModelRow(table); // correct even when sorted or filtered
        return row < 0 ? null : volunteers.get(row);
    }

    private void fillFormFromSelection() {
        Volunteer selected = selectedVolunteer();
        if (selected == null) {
            return;
        }
        nameField.setText(selected.getName());
        emailField.setText(selected.getEmail());
        phoneField.setText(selected.getPhone());
        skillsField.setText(selected.getSkills());
        availabilityField.setText(selected.getAvailability());
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        skillsField.setText("");
        availabilityField.setText("");
    }
}
