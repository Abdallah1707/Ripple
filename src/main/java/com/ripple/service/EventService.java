package com.ripple.service;

import com.ripple.logging.ApplicationLogger;
import com.ripple.model.Event;
import com.ripple.observer.EventObserver;
import com.ripple.observer.EventSubject;
import com.ripple.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves events. Also the <b>Subject</b> in the Observer pattern for events: when an event's
 * date or location changes, it tells every attached {@link EventObserver}.
 *
 * <p>Only those two changes matter to attendees (they decide when and where to go), so a new
 * name or description does not notify anyone. Like {@link DonationService}, this class does
 * not know what the observers do; today one of them messages the attendees.
 */
public class EventService implements EventSubject {

    private final List<EventObserver> observers = new ArrayList<>();
    private final EventRepository eventRepository;

    /**
     * Creates the service.
     *
     * @param eventRepository event storage
     */
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // ---------------- Observer pattern: the Subject's methods ----------------

    @Override
    public void registerObserver(EventObserver observer) {
        if (observer != null && !observers.contains(observer)) { // registering twice has no effect
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(EventObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Event event, String change) {
        // Loop over a copy so an observer may remove itself while being notified.
        for (EventObserver observer : new ArrayList<>(observers)) {
            try {
                observer.update(event, change);
            } catch (RuntimeException e) {
                // One failing observer must not stop the others, nor undo the saved change.
                ApplicationLogger.getInstance().error(
                        observer.getClass().getSimpleName() + " failed: " + e.getMessage());
            }
        }
    }

    /**
     * Tells whether an observer is currently attached.
     *
     * @param observer the observer to look for
     * @return {@code true} if it is attached
     */
    public boolean isRegistered(EventObserver observer) {
        return observers.contains(observer);
    }

    // ---------------- Events ----------------

    /**
     * Saves a new event. Nobody is notified: a new event has no attendees yet.
     *
     * @param event the event to save (already checked by the controller)
     * @return the same event, now with its id
     */
    public Event addEvent(Event event) {
        return eventRepository.insert(event);
    }

    /**
     * Saves new values for an existing event, and notifies the observers if its date or
     * location changed.
     *
     * @param event the event with its id and new values (already checked by the controller)
     * @return {@code true} if the date or location changed, so the observers were notified
     * @throws ValidationException if there is no event with that id
     */
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

    /** Describes a new date and/or location in words, or returns "" if neither changed. */
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
