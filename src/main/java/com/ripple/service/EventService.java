package com.ripple.service;

import com.ripple.logging.ApplicationLogger;
import com.ripple.model.Event;
import com.ripple.observer.EventObserver;
import com.ripple.observer.EventSubject;
import com.ripple.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves events. Also the observer subject for events: notifies observers
 * when an event's date or location changes.
 */
public class EventService implements EventSubject {

    private final List<EventObserver> observers = new ArrayList<>();
    private final EventRepository eventRepository;

    /** Creates the service. */
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // ---- observer methods ----

    @Override
    public void registerObserver(EventObserver observer) {
        if (observer != null && !observers.contains(observer)) { // no duplicates
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(EventObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Event event, String change) {
        // loop over a copy in case an observer removes itself
        for (EventObserver observer : new ArrayList<>(observers)) {
            try {
                observer.update(event, change);
            } catch (RuntimeException e) {
                // one failing observer shouldn't stop the rest
                ApplicationLogger.getInstance().error(
                        observer.getClass().getSimpleName() + " failed: " + e.getMessage());
            }
        }
    }

    /** True if the observer is registered. */
    public boolean isRegistered(EventObserver observer) {
        return observers.contains(observer);
    }

    // ---- events ----

    /** Saves a new event. No one is notified since it has no attendees yet. */
    public Event addEvent(Event event) {
        return eventRepository.insert(event);
    }

    /** Updates an event. Returns true if the date or location changed and observers were notified. */
    public boolean updateEvent(Event event) {
        Event before = eventRepository.findById(event.getId())
                .orElseThrow(() -> new ValidationException("This event no longer exists"));
        eventRepository.update(event);

        String change = describeChange(before, event);
        if (change.isEmpty()) {
            return false;
        }
        notifyObservers(event, change);
        return true;
    }

    /** Describes what changed, or "" if nothing did. */
    private static String describeChange(Event before, Event after) {
        List<String> changes = new ArrayList<>();
        if (!before.getEventDate().equals(after.getEventDate())) {
            changes.add("date changed from " + before.getEventDate() + " to " + after.getEventDate());
        }
        if (!before.getLocation().equals(after.getLocation())) {
            changes.add("location changed from '" + before.getLocation() + "' to '" + after.getLocation() + "'");
        }
        return String.join("; ", changes);
    }
}
