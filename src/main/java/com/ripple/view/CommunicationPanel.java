package com.ripple.view;

import com.ripple.communication.MessageLogEntry;
import com.ripple.communication.SmsStrategy;
import com.ripple.controller.Audience;
import com.ripple.controller.CommunicationController;
import com.ripple.controller.EventController;
import com.ripple.controller.Result;
import com.ripple.model.ContactChannel;
import com.ripple.model.Event;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
 * The Communication screen: send a message to a whole group by Email or SMS, and see the
 * message log.
 *
 * <p>A <b>View</b> in MVC: it only shows things and calls {@link CommunicationController}
 * (and {@link EventController} for the list of events). The outcome of every send is shown
 * in the status bar. Sending is simulated, and the screen says so.
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

    /**
     * Builds the screen.
     *
     * @param communicationController the communication controller
     * @param eventController         gives the list of events for "Attendees of one event"
     * @param statusBar               where to show the outcome of each send
     */
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
        logPanel.add(new JScrollPane(table), BorderLayout.CENTER);
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

    /** The form: who, which channel, subject and message, the Send button and the "simulated" note. */
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

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> send());
        addRow(form, 6, new JLabel(), sendButton);

        JLabel simulatedNote = new JLabel("Simulated — no real email or SMS is sent.");
        simulatedNote.setForeground(new Color(180, 90, 0));
        simulatedNote.setFont(simulatedNote.getFont().deriveFont(Font.BOLD));
        addRow(form, 7, new JLabel(), simulatedNote);
        return form;
    }

    /** Adds one "label + field" row to a form that uses GridBagLayout. */
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

    /** Shows how long the message is, and the SMS limit when SMS is chosen. */
    private void updateLengthLabel() {
        int length = bodyArea.getText().length();
        boolean sms = channelBox.getSelectedItem() == ContactChannel.SMS;
        lengthLabel.setText(sms ? length + " / " + SmsStrategy.MAX_LENGTH + " characters (SMS limit)"
                : length + " characters");
        lengthLabel.setForeground(sms && length > SmsStrategy.MAX_LENGTH ? Color.RED : Color.DARK_GRAY);
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
        refresh(); // skipped attempts are logged too, so reload the log either way
    }

    /** Reloads the events and the message log. */
    @Override
    public void refresh() {
        Event selectedEvent = (Event) eventBox.getSelectedItem();
        eventBox.removeAllItems();
        for (Event event : eventController.listEvents()) {
            eventBox.addItem(event);
            if (selectedEvent != null && event.getId() == selectedEvent.getId()) {
                eventBox.setSelectedItem(event); // keep the previous choice
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
