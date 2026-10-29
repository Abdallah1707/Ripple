package com.ripple.app;

import com.ripple.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of Ripple and its <b>composition root</b>: the one place that creates
 * the application's objects and wires them together.
 *
 * <p>In step 1 there is nothing to wire yet, so it only opens the control panel.
 * Later steps add the database, repositories, services, observers and controllers
 * here, in the order described in {@code docs/architecture.md} section 4.
 */
public final class Main {

    private Main() {
        // Not meant to be instantiated.
    }

    /**
     * Starts Ripple.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        useNativeLookAndFeel();
        // Swing is not thread-safe: every UI object must be created on the
        // Event Dispatch Thread, which is what invokeLater does.
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    /** Makes Ripple look like a normal Windows (or Linux) app instead of Swing's default "Metal" theme. */
    private static void useNativeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // The default look and feel still works, so this is not fatal.
            System.err.println("Could not use the system look and feel: " + e.getMessage());
        }
    }
}
