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
import java.util.List;
import java.util.Map;

/**
 * The main window: sidebar on the left, the current screen in the middle,
 * and the status bar at the bottom. Main adds the screens with addScreen.
 */
public class MainFrame extends JFrame {

    /** Screen names. */
    public static final String DASHBOARD = "Dashboard";
    public static final String DONATIONS = "Donations";
    public static final String VOLUNTEERS = "Volunteers";
    public static final String EVENTS = "Events";
    public static final String BENEFICIARIES = "Beneficiaries";
    public static final String COMMUNICATION = "Communication";
    public static final String LOOKUPS = "Lookup Management";

    /** All screens in sidebar order. */
    private static final List<String> SCREEN_NAMES =
            List.of(DASHBOARD, DONATIONS, VOLUNTEERS, EVENTS, BENEFICIARIES, COMMUNICATION, LOOKUPS);

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final StatusBar statusBar = new StatusBar();
    /** The panel for each screen name. */
    private final Map<String, JPanel> cardsByName = new HashMap<>();
    /** Screens that reload when shown. */
    private final Map<String, Refreshable> screens = new HashMap<>();
    private String currentScreen;

    /** Builds the empty window. Screens are added by Main. */
    public MainFrame() {
        super("Ripple — NGO Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        showScreen(DASHBOARD);
        statusBar.showResult(Result.ok("Ripple started."));
        pack();
        setLocationRelativeTo(null); // center on screen
    }

    /** The sidebar with one button per screen. */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 60, 90));
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        // wide enough for "Lookup Management"
        sidebar.setPreferredSize(new Dimension(200, 0));

        JLabel title = new JLabel("Ripple");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(title);
        sidebar.add(Box.createVerticalStrut(20));

        // only one button pressed at a time
        ButtonGroup group = new ButtonGroup();
        for (String name : SCREEN_NAMES) {
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
     * Adds a screen for a sidebar button, or replaces the old one.
     * Refreshable screens reload every time they're opened.
     */
    public void addScreen(String name, JPanel screen) {
        if (!SCREEN_NAMES.contains(name)) {
            throw new IllegalArgumentException("Unknown screen: " + name);
        }
        JPanel old = cardsByName.put(name, screen);
        if (old != null) {
            content.remove(old);
        }
        content.add(screen, name);
        if (screen instanceof Refreshable refreshable) {
            screens.put(name, refreshable);
        } else {
            screens.remove(name);
        }
        if (name.equals(currentScreen)) {
            showScreen(name); // already showing, so swap it now
        }
    }

    /** The shared status bar. */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /** Shows a screen and reloads it if it can. */
    private void showScreen(String name) {
        currentScreen = name;
        cards.show(content, name);
        Refreshable screen = screens.get(name);
        if (screen != null) {
            screen.refresh();
        }
    }
}
