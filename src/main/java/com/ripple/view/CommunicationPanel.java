package com.ripple.view;

import com.ripple.communication.MessageLogEntry;
import com.ripple.communication.SmsStrategy;
import com.ripple.communication.SocialMediaStrategy;
import com.ripple.controller.Audience;
import com.ripple.controller.CommunicationController;
import com.ripple.controller.EventController;
import com.ripple.controller.Result;
import com.ripple.model.ContactChannel;
import com.ripple.model.Event;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;

/**
 * The Communication screen: send a message to a group and see the message log.
 */
public class CommunicationPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CommunicationController communicationController;
    private final EventController eventController;
    private final StatusBar statusBar;

    private final JComboBox<Audience> audienceBox = new JComboBox<>(Audience.values());
    private final JComboBox<Event> eventBox = new JComboBox<>();
    private final JComboBox<ContactChannel> channelBox = new JComboBox<>(ContactChannel.values());
    private final JTextField subjectField = new JTextField(30);
    private final JTextArea bodyArea = new JTextArea(4, 30);
    private final JLabel lengthLabel = new JLabel();
    private final DefaultTableModel tableModel;

    /** Builds the screen. */
    public CommunicationPanel(CommunicationController communicationController, EventController eventController,
                              StatusBar statusBar) {
        super(new BorderLayout(0, 8));
        this.communicationController = communicationController;
        this.eventController = eventController;
        this.statusBar = statusBar;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildForm(), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"Sent at", "Channel", "Recipient", "Subject", "Message", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Message log"));
        logPanel.add(FormHelper.tableWithSearch(table), BorderLayout.CENTER);
        add(logPanel, BorderLayout.CENTER);

        audienceBox.addActionListener(e -> eventBox.setEnabled(audienceBox.getSelectedItem() == Audience.EVENT_ATTENDEES));
        eventBox.setEnabled(false);
        channelBox.addActionListener(e -> updateLengthLabel());
        bodyArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLengthLabel();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLengthLabel();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLengthLabel();
            }
        });
        updateLengthLabel();
    }

    /** The send form. */
    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Send a message to a group"));
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);

        addRow(form, 0, new JLabel("Send to"), audienceBox);
        addRow(form, 1, new JLabel("Event"), eventBox);
        addRow(form, 2, new JLabel("Channel"), channelBox);
        addRow(form, 3, new JLabel("Subject"), subjectField);
        addRow(form, 4, new JLabel("Message"), new JScrollPane(bodyArea));
        addRow(form, 5, new JLabel(), lengthLabel);

        FormHelper.placeholder(subjectField, "e.g. Thank you for your support");
        subjectField.setToolTipText("Used for email; SMS messages have no subject");
        channelBox.setToolTipText("SMS messages are limited to " + SmsStrategy.MAX_LENGTH + " characters, social media "
                + "messages to " + SocialMediaStrategy.MAX_LENGTH + "; only donors have a social handle");
        FormHelper.onEnter(this::send, subjectField);
        addRow(form, 6, new JLabel(), FormHelper.button("Send",
                "Send this message to everyone in the chosen group", this::send));

        JLabel simulatedNote = new JLabel("Simulated — no real email or SMS is sent.");
        simulatedNote.setForeground(new Color(180, 90, 0));
        simulatedNote.setFont(simulatedNote.getFont().deriveFont(Font.BOLD));
        addRow(form, 7, new JLabel(), simulatedNote);
        return form;
    }

    /** Adds a label and field row. */
    private void addRow(JPanel form, int row, JLabel label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        form.add(label, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(field, c);
    }

    /** Shows the message length and the limit for SMS or Social. */
    private void updateLengthLabel() {
        int length = bodyArea.getText().length();
        ContactChannel channel = (ContactChannel) channelBox.getSelectedItem();
        int limit = channel == ContactChannel.SMS ? SmsStrategy.MAX_LENGTH
                : channel == ContactChannel.SOCIAL ? SocialMediaStrategy.MAX_LENGTH
                : 0; // no limit for email
        String text = limit == 0 ? length + " characters"
                : length + " / " + limit + " characters (" + channel + " limit)";
        if (channel == ContactChannel.SOCIAL) {
            text += " · only donors with a social handle receive it";
        }
        lengthLabel.setText(text);
        lengthLabel.setForeground(limit > 0 && length > limit ? Color.RED : Color.DARK_GRAY);
    }

    private void send() {
        Audience audience = (Audience) audienceBox.getSelectedItem();
        Event event = (Event) eventBox.getSelectedItem();
        ContactChannel channel = (ContactChannel) channelBox.getSelectedItem();
        Result result = communicationController.sendToGroup(audience,
                event == null ? null : event.getId(),
                channel == null ? null : channel.name(),
                subjectField.getText(), bodyArea.getText());
        statusBar.showResult(result);
        refresh(); // skipped messages are logged too
    }

    /** Reloads the events and message log. */
    @Override
    public void refresh() {
        Event selectedEvent = (Event) eventBox.getSelectedItem();
        eventBox.removeAllItems();
        for (Event event : eventController.listEvents()) {
            eventBox.addItem(event);
            if (selectedEvent != null && event.getId() == selectedEvent.getId()) {
                eventBox.setSelectedItem(event); // keep the old selection
            }
        }

        tableModel.setRowCount(0);
        for (MessageLogEntry entry : communicationController.listMessageLog()) {
            tableModel.addRow(new Object[]{
                    entry.sentAt().format(DATE_FORMAT),
                    entry.channel(),
                    entry.recipient(),
                    entry.subject(),
                    entry.body(),
                    entry.status()});
        }
    }
}
