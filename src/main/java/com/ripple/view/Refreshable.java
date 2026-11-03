package com.ripple.view;

/**
 * A screen that reloads its data every time it's opened.
 */
public interface Refreshable {

    /** Reloads the data. */
    void refresh();
}
