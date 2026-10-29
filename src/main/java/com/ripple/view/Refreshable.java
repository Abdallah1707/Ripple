package com.ripple.view;

/**
 * A screen that can reload its data.
 *
 * <p>{@link MainFrame} calls {@link #refresh()} every time the user opens the screen,
 * so tables always show what is currently in the database (for example, a donation
 * type added in Lookup Management appears in the Donations form right away).
 */
public interface Refreshable {

    /** Reloads the data shown on this screen. */
    void refresh();
}
