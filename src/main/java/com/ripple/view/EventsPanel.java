package com.ripple.view;

import com.ripple.controller.EventController;
import com.ripple.controller.LookupController;
import com.ripple.controller.Result;
import com.ripple.model.Attendee;
import com.ripple.model.Event;
import com.ripple.model.EventCategory;
import com.ripple.model.Volunteer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * The Events screen.
 *
 * <p>Left: a table of events and a form with Add / Update / Delete / Clear buttons (the
 * category is picked from the Event Categories lookup). Right: for the selected event, its
 * attendees (with a small form to register one) and the volunteers assigned to it.
 *
 * <p>A <b>View</b> in MVC: it only shows things and calls {@link EventController}
 * (and {@link LookupController} for the category list). The outcome of every action is
 * shown in the status bar.
 */
public class EventsPanel extends JPanel implements Refreshable {

    private final EventController eventController;
    private final LookupController lookupController;
    private final StatusBar statusBar;

    // --- events table and form ---
    private final DefaultTableModel eventsModel;
    private final JTable eventsTable;
    private final JTextField nameField = new JTextField(16);
    private final JComboBox<EventCategory> categoryBox = new JComboBox<>();
    private final JTextField dateField = new JTextField(10);
    private final JTextField locationField = new JTextField(16);
    private final JTextField descriptionField = new JTextField(16);
    /** The events currently shown, in the same order as the table rows. */
    private List<Event> events = new ArrayList<>();

    // --- details of the selected event ---
    private final JLabel detailsTitle = new JLabel("Select an event to see its attendees and volunteers.");
    private final DefaultTableModel attendeesModel;
    private final DefaultTableModel volunteersModel;
    private final JTextField attendeeNameField = new JTextField(10);
    private final JTextField attendeeEmailField = new JTextField(10);
    private final JTextField attendeePhoneField = new JTextField(9);

    /**
     * Builds the screen.
     *
     * @param eventController  the event controller
     * @param lookupController the lookup controller (fills the category list)
     * @param statusBar        where to show the outcome of each action
     */
    public EventsPanel(EventController eventController, LookupController lookupController, StatusBar statusBar) {
        super(new BorderLayout());
        this.eventController = eventController;
        this.lookupController = lookupController;
        this.statusBar = statusBar;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        eventsModel = readOnlyModel("Name", "Category", "Date", "Location");
        eventsTable = new JTable(eventsModel);
        eventsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventsTable.setRowHeight(24);
        eventsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFormFromSelection();
                showDetails();
            }
        });
        FormHelper.onDoubleClick(eventsTable, nameField::requestFocusInWindow); // edit the row in the form

        FormHelper.placeholder(nameField, "e.g. Winter clothes drive");
        FormHelper.placeholder(dateField, "2026-12-15");
        FormHelper.placeholder(locationField, "e.g. Nasr City hall");
        FormHelper.placeholder(descriptionField, "Optional");
        FormHelper.placeholder(attendeeNameField, "Full name");
        FormHelper.placeholder(attendeeEmailField, "name@example.com");
        FormHelper.placeholder(attendeePhoneField, "01012345678");
        FormHelper.onEnter(this::onSave, nameField, dateField, locationField, descriptionField);
        FormHelper.onEnter(this::onRegisterAttendee, attendeeNameField, attendeeEmailField, attendeePhoneField);
        attendeesModel = readOnlyModel("Name", "Email", "Phone");
        volunteersModel = readOnlyModel("Name", "Email", "Phone");

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.add(FormHelper.tableWithSearch(eventsTable), BorderLayout.CENTER);
        left.add(buildEventForm(), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, buildDetailsPanel());
        split.setResizeWeight(0.55);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        categoryBox.removeAllItems();
        for (EventCategory category : lookupController.listEventCategories()) {
            categoryBox.addItem(category);
        }
        events = eventController.listEvents();
        eventsModel.setRowCount(0);
        for (Event event : events) {
            eventsModel.addRow(new Object[]{event.getName(), event.getCategory().getName(),
                    event.getEventDate().toString(), event.getLocation()});
        }
        clearForm();
    }

    // ---------------- layout ----------------

    private JPanel buildEventForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Add or edit an event"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        dateField.setToolTipText("Year-month-day, e.g. 2026-12-15");
        // One field per row: the form sits in the left half of a split pane, so two columns
        // of fields would not fit and GridBagLayout would squeeze every field to nothing.
        addRow(form, c, 0, "Name:", nameField);
        addRow(form, c, 1, "Category:", categoryBox);
        addRow(form, c, 2, "Date (yyyy-MM-dd):", dateField);
        addRow(form, c, 3, "Location:", locationField);
        addRow(form, c, 4, "Description:", descriptionField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(FormHelper.button("Add", "Save the form as a new event", this::onAdd));
        buttons.add(FormHelper.button("Update", "Save the form over the selected event", this::onUpdate));
        buttons.add(FormHelper.button("Delete", "Delete the selected event", this::onDelete));
        buttons.add(FormHelper.button("Clear", "Empty the form and unselect the row", this::clearForm));
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        form.add(buttons, c);
        return form;
    }

    private JPanel buildDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        detailsTitle.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(detailsTitle);
        panel.add(javax.swing.Box.createVerticalStrut(6));

        JScrollPane attendeesScroll = new JScrollPane(new JTable(attendeesModel));
        attendeesScroll.setBorder(BorderFactory.createTitledBorder("Attendees"));
        attendeesScroll.setAlignmentX(LEFT_ALIGNMENT);
        attendeesScroll.setPreferredSize(new Dimension(300, 150));
        panel.add(attendeesScroll);

        JPanel attendeeForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        attendeeForm.setAlignmentX(LEFT_ALIGNMENT);
        attendeeForm.add(new JLabel("Name:"));
        attendeeForm.add(attendeeNameField);
        attendeeForm.add(new JLabel("Email:"));
        attendeeForm.add(attendeeEmailField);
        attendeeForm.add(new JLabel("Phone:"));
        attendeeForm.add(attendeePhoneField);
        attendeeForm.add(FormHelper.button("Register attendee", "Register this person for the selected event",
                this::onRegisterAttendee));
        panel.add(attendeeForm);
        panel.add(javax.swing.Box.createVerticalStrut(6));

        JScrollPane volunteersScroll = new JScrollPane(new JTable(volunteersModel));
        volunteersScroll.setBorder(BorderFactory.createTitledBorder("Assigned volunteers"));
        volunteersScroll.setAlignmentX(LEFT_ALIGNMENT);
        volunteersScroll.setPreferredSize(new Dimension(300, 130));
        panel.add(volunteersScroll);
        return panel;
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** Adds one "label + field" row; the field stretches to the width of the form. */
    private static void addRow(JPanel form, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridy = row;
        c.gridwidth = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(field, c);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
    }

    // ---------------- actions ----------------

    /** Enter in the event form: update the selected event, or add a new one if none is selected. */
    private void onSave() {
        if (selectedEvent() == null) {
            onAdd();
        } else {
            onUpdate();
        }
    }

    private void onAdd() {
        showAndRefresh(eventController.saveEvent(null, nameField.getText(), selectedCategoryId(),
                dateField.getText(), locationField.getText(), descriptionField.getText()));
    }

    private void onUpdate() {
        Event selected = selectedEvent();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a row to update first."));
            return;
        }
        showAndRefresh(eventController.saveEvent(selected.getId(), nameField.getText(), selectedCategoryId(),
                dateField.getText(), locationField.getText(), descriptionField.getText()));
    }

    private void onDelete() {
        Event selected = selectedEvent();
        if (selected == null) {
            statusBar.showResult(Result.fail("Select a row to delete first."));
            return;
        }
        if (FormHelper.confirmDelete(this,
                "Delete '" + selected.getName() + "'? Its attendees and volunteer assignments are removed too.")) {
            showAndRefresh(eventController.deleteEvent(selected.getId()));
        }
    }

    private void onRegisterAttendee() {
        Event selected = selectedEvent();
        Result result = eventController.registerAttendee(selected == null ? 0 : selected.getId(),
                attendeeNameField.getText(), attendeeEmailField.getText(), attendeePhoneField.getText());
        statusBar.showResult(result);
        if (result.success()) {
            attendeeNameField.setText("");
            attendeeEmailField.setText("");
            attendeePhoneField.setText("");
            showDetails();
        }
    }

    /** Shows the result; on success reloads the table so it matches the database. */
    private void showAndRefresh(Result result) {
        statusBar.showResult(result);
        if (result.success()) {
            refresh();
        }
    }

    // ---------------- selection ----------------

    private Event selectedEvent() {
        int row = FormHelper.selectedModelRow(eventsTable); // correct even when sorted or filtered
        return row < 0 ? null : events.get(row);
    }

    private int selectedCategoryId() {
        EventCategory category = (EventCategory) categoryBox.getSelectedItem();
        return category == null ? 0 : category.getId();
    }

    private void fillFormFromSelection() {
        Event selected = selectedEvent();
        if (selected == null) {
            return;
        }
        nameField.setText(selected.getName());
        dateField.setText(selected.getEventDate().toString());
        locationField.setText(selected.getLocation());
        descriptionField.setText(selected.getDescription());
        for (int i = 0; i < categoryBox.getItemCount(); i++) {
            if (categoryBox.getItemAt(i).getId() == selected.getCategory().getId()) {
                categoryBox.setSelectedIndex(i);
                break;
            }
        }
    }

    /** Reloads the attendee and volunteer lists for the selected event (or empties them). */
    private void showDetails() {
        attendeesModel.setRowCount(0);
        volunteersModel.setRowCount(0);
        Event selected = selectedEvent();
        if (selected == null) {
            detailsTitle.setText("Select an event to see its attendees and volunteers.");
            return;
        }
        detailsTitle.setText("Event: " + selected.getName());
        for (Attendee a : eventController.listAttendees(selected.getId())) {
            attendeesModel.addRow(new Object[]{a.getName(), a.getEmail(), a.getPhone()});
        }
        for (Volunteer v : eventController.listVolunteers(selected.getId())) {
            volunteersModel.addRow(new Object[]{v.getName(), v.getEmail(), v.getPhone()});
        }
    }

    private void clearForm() {
        eventsTable.clearSelection();
        nameField.setText("");
        dateField.setText("");
        locationField.setText("");
        descriptionField.setText("");
        if (categoryBox.getItemCount() > 0) {
            categoryBox.setSelectedIndex(0);
        }
        showDetails();
    }
}
