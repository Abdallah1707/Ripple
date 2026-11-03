package com.ripple.view;

import com.ripple.controller.Result;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;

/**
 * The bar at the bottom of the window. Green for success, red for errors (CR6).
 */
public class StatusBar extends JLabel {

    private static final Color SUCCESS_COLOR = new Color(0, 120, 60);
    private static final Color ERROR_COLOR = new Color(180, 30, 30);

    /** Creates an empty status bar. */
    public StatusBar() {
        super(" "); // keeps the height when empty
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    /** Shows the result of an action. */
    public void showResult(Result result) {
        setForeground(result.success() ? SUCCESS_COLOR : ERROR_COLOR);
        setText(result.message());
    }
}
