package com.ripple.observer;

import com.ripple.model.Event;

/**
 * Gets told when an event's date or location changes.
 */
public interface EventObserver {

    /** Called after a changed event is saved. */
    void update(Event event, String change);
}
