package com.ripple.observer;

import com.ripple.model.Event;

/**
 * Observer interface in the Observer pattern for events: anything that wants to know when an
 * event's date or location changes implements this interface and is attached to the
 * {@link EventSubject}.
 *
 * <p>Like {@link DonationObserver} it uses the <b>push model</b>: the subject passes the
 * changed event and a description of the change to {@link #update(Event, String)}.
 */
public interface EventObserver {

    /**
     * Called by the subject right after a changed event has been saved.
     *
     * @param event  the event with its new values
     * @param change what changed, in words, e.g. {@code "location changed from 'Cairo' to 'Giza'"}
     */
    void update(Event event, String change);
}
