package com.ripple.view;

import com.ripple.controller.Result;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/**
 * The <b>User Control Panel</b>: Ripple's main window.
 *
 * <p>Layout:
 * <ul>
 *   <li>left — a sidebar with one button per module;</li>
 *   <li>centre — a {@link CardLayout} that shows one screen at a time;</li>
 *   <li>bottom — the {@link StatusBar}.</li>
 * </ul>
 *
 * <p>This is a <b>View</b> in MVC: it only arranges screens and switches between them.
 * Screens that are not built yet show which step adds them, so no button pretends
 * to do something that does not exist.
 */
public class MainFrame extends JFrame {

    private static final String DASHBOARD = "Dashboard";
    private static final String DONATIONS = "Donations";
    private static final String VOLUNTEERS = "Volunteers";
    private static final String EVENTS = "Events";
    private static final String BENEFICIARIES = "Beneficiaries";
    private static final String COMMUNICATION = "Communication";
    private static final String LOOKUPS = "Lookup Management";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final StatusBar statusBar = new StatusBar();
    /** Screens that can reload their data when shown. Placeholders are not in here. */
    private final Map<String, Refreshable> screens = new HashMap<>();

    /** Builds the window and all its screens. */
    public MainFrame() {
        super("Ripple — NGO Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));

        addPlaceholder(DASHBOARD, "Totals and recent activity", 5);
        addPlaceholder(DONATIONS, "Donors, donations and receipts", 5);
        addPlaceholder(VOLUNTEERS, "Volunteer profiles and event assignment", 5);
        addPlaceholder(EVENTS, "Events, attendees and assigned volunteers", 5);
        addPlaceholder(BENEFICIARIES, "Beneficiaries and their needs", 5);
        addPlaceholder(COMMUNICATION, "Email / SMS messages to groups", 5);
        addPlaceholder(LOOKUPS, "Donation Types and Event Categories", 2);

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        showScreen(DASHBOARD);
        statusBar.showResult(Result.ok("Ripple started."));
        pack();
        setLocationRelativeTo(null); // centre on screen
    }

    /** Creates the left-hand navigation: a title and one toggle button per screen. */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 60, 90));
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        // BorderLayout.WEST uses the preferred width; wide enough for "Lookup Management".
        sidebar.setPreferredSize(new Dimension(200, 0));

        JLabel title = new JLabel("Ripple");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(title);
        sidebar.add(Box.createVerticalStrut(20));

        // A ButtonGroup keeps exactly one toggle button pressed: the current screen.
        ButtonGroup group = new ButtonGroup();
        String[] names = {DASHBOARD, DONATIONS, VOLUNTEERS, EVENTS, BENEFICIARIES, COMMUNICATION, LOOKUPS};
        for (String name : names) {
            JToggleButton button = new JToggleButton(name);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFocusPainted(false);
            button.addActionListener(e -> showScreen(name));
            button.setSelected(name.equals(DASHBOARD));
            group.add(button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(6));
        }
        return sidebar;
    }

    /**
     * Adds a screen that is not built yet. It says what it will contain and in which
     * implementation step it arrives.
     */
    private void addPlaceholder(String name, String purpose, int step) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("<html><div style='text-align:center'>"
                + "<h2>" + name + "</h2>"
                + "<p>" + purpose + "</p><br>"
                + "<p><i>Available after step " + step + ".</i></p></div></html>",
                SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        content.add(panel, name);
    }

    /** Shows the named screen and, if it can, reloads its data. */
    private void showScreen(String name) {
        cards.show(content, name);
        Refreshable screen = screens.get(name);
        if (screen != null) {
            screen.refresh();
        }
    }
}
