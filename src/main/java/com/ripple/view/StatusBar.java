package com.ripple.view;

import com.ripple.controller.Result;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;

/**
 * The line at the bottom of the main window that tells the user whether their
 * last action worked: green for success, red for failure (client requirement CR6).
 *
 * <p>One instance is created by {@link MainFrame} and passed to every screen.
 */
public class StatusBar extends JLabel {

    private static final Color SUCCESS_COLOR = new Color(0, 120, 60);
    private static final Color ERROR_COLOR = new Color(180, 30, 30);

    /** Creates an empty status bar. */
    public StatusBar() {
        super(" "); // a single space keeps the bar's height when there is no message
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    /**
     * Shows the outcome of an action.
     *
     * @param result the controller's result
     */
    public void showResult(Result result) {
        setForeground(result.success() ? SUCCESS_COLOR : ERROR_COLOR);
        setText(result.message());
    }
}
