package com.ripple.observer;

import com.ripple.model.Event;

/**
 * Keeps a list of event observers and notifies them.
 */
public interface EventSubject {

    /** Adds an observer. Adding twice does nothing. */
    void registerObserver(EventObserver observer);

    /** Removes an observer. */
    void removeObserver(EventObserver observer);

    /** Notifies all observers that an event changed. */
    void notifyObservers(Event event, String change);
}
