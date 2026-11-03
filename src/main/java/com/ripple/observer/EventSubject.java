package com.ripple.observer;

import com.ripple.model.Event;

/**
 * Subject interface in the Observer pattern for events: an object that keeps a list of
 * {@link EventObserver}s and tells all of them when an event's date or location changes.
 * {@code EventService} is the concrete subject. It has the same shape as
 * {@link DonationSubject}: the lab slides' {@code registerObserver}, {@code removeObserver},
 * {@code notifyObservers}.
 */
public interface EventSubject {

    /**
     * Adds an observer. Registering the same observer twice has no effect.
     *
     * @param observer the observer to add
     */
    void registerObserver(EventObserver observer);

    /**
     * Removes an observer, so it no longer hears about event changes.
     *
     * @param observer the observer to remove
     */
    void removeObserver(EventObserver observer);

    /**
     * Tells every attached observer that an event changed.
     *
     * @param event  the event, already saved with its new values
     * @param change what changed, in words, e.g. {@code "date changed from 2026-12-15 to 2026-12-20"}
     */
    void notifyObservers(Event event, String change);
}
