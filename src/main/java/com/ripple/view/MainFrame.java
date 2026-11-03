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
 *
 * <p><b>Adding a screen:</b> {@code Main} plugs each screen in with
 * {@link #addScreen(String, JPanel)}, using one of the screen-name constants:
 * <pre>{@code
 * frame.addScreen(MainFrame.LOOKUPS, new LookupPanel(lookupController, frame.getStatusBar()));
 * }</pre>
 */
public class MainFrame extends JFrame {

    /** Screen names. Use these with {@link #addScreen(String, JPanel)}. */
    public static final String DASHBOARD = "Dashboard";
    public static final String DONATIONS = "Donations";
    public static final String VOLUNTEERS = "Volunteers";
    public static final String EVENTS = "Events";
    public static final String BENEFICIARIES = "Beneficiaries";
    public static final String COMMUNICATION = "Communication";
    public static final String LOOKUPS = "Lookup Management";

    /** Every screen, in the order of the sidebar buttons. */
    private static final List<String> SCREEN_NAMES =
            List.of(DASHBOARD, DONATIONS, VOLUNTEERS, EVENTS, BENEFICIARIES, COMMUNICATION, LOOKUPS);

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final StatusBar statusBar = new StatusBar();
    /** The screen plugged in for each screen name. */
    private final Map<String, JPanel> cardsByName = new HashMap<>();
    /** Screens that can reload their data when shown. */
    private final Map<String, Refreshable> screens = new HashMap<>();
    private String currentScreen;

    /** Builds the empty window: sidebar, content area and status bar. Screens are added by {@code Main}. */
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
     * Plugs a screen in for the sidebar button called {@code name}. Adding a screen for a name
     * that already has one replaces it.
     *
     * <p>If the screen implements {@link Refreshable}, it is refreshed every time the
     * user opens it.
     *
     * @param name   one of the screen-name constants, e.g. {@link #LOOKUPS}
     * @param screen the panel to show for that sidebar button
     * @throws IllegalArgumentException if {@code name} is not one of the constants
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
            showScreen(name); // the user is looking at it: swap it in now
        }
    }

    /**
     * The shared status bar. Pass it to each screen so it can show controller results.
     *
     * @return the status bar at the bottom of the window
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /** Shows the named screen and, if it can, reloads its data. */
    private void showScreen(String name) {
        currentScreen = name;
        cards.show(content, name);
        Refreshable screen = screens.get(name);
        if (screen != null) {
            screen.refresh();
        }
    }
}
