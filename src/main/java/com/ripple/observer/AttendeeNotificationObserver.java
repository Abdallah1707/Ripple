package com.ripple.observer;

import com.ripple.communication.CommunicationService;
import com.ripple.communication.CommunicationStrategy;
import com.ripple.model.Attendee;
import com.ripple.model.Event;
import com.ripple.repository.EventRepository;

import java.util.List;

/**
 * Concrete observer for events: when an event's date or location changes, it messages every
 * attendee of that event about the change.
 *
 * <p>Attendees have no preferred channel, so each one gets the first strategy in the list
 * that has a valid address for them (Email, then SMS, as built in {@code Main}). Someone with
 * neither is still "sent" by the first strategy, so the failure appears in the message log
 * instead of being silently skipped. It uses the Strategy pattern from inside the Observer.
 */
public class AttendeeNotificationObserver implements EventObserver {

    private final EventRepository eventRepository;
    private final CommunicationService communicationService;
    private final List<CommunicationStrategy> strategies;

    /**
     * Creates the observer.
     *
     * @param eventRepository      where the event's attendees are read from
     * @param communicationService sends and logs the messages; give it its own instance, so
     *                             switching strategies here never changes the Communication screen's
     * @param strategies           the channels to try for each attendee, in order of preference
     * @throws IllegalArgumentException if no strategy is given
     */
    public AttendeeNotificationObserver(EventRepository eventRepository, CommunicationService communicationService,
                                        List<CommunicationStrategy> strategies) {
        if (strategies.isEmpty()) {
            throw new IllegalArgumentException("At least one communication strategy is needed");
        }
        this.eventRepository = eventRepository;
        this.communicationService = communicationService;
        this.strategies = List.copyOf(strategies);
    }

    @Override
    public void update(Event event, String change) {
        // Short on purpose, so it also fits in one SMS.
        String body = event.getName() + ": " + change + ". - Ripple";
        for (Attendee attendee : eventRepository.findAttendees(event.getId())) {
            communicationService.setStrategy(strategyFor(attendee));
            communicationService.send(attendee, "Event update: " + event.getName(), body);
        }
    }

    /** The first strategy with a valid address for this attendee, or the first one if none has. */
    private CommunicationStrategy strategyFor(Attendee attendee) {
        for (CommunicationStrategy strategy : strategies) {
            if (strategy.isValidAddress(strategy.addressOf(attendee))) {
                return strategy;
            }
        }
        return strategies.get(0);
    }
}
