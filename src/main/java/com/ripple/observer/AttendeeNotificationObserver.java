package com.ripple.observer;

import com.ripple.communication.CommunicationService;
import com.ripple.communication.CommunicationStrategy;
import com.ripple.model.Attendee;
import com.ripple.model.Event;
import com.ripple.repository.EventRepository;

import java.util.List;

/**
 * Messages all attendees when an event's date or location changes.
 * Each attendee gets the first strategy that has a valid address for them
 * (email, then SMS). If none works, the first one is used so the failure is logged.
 */
public class AttendeeNotificationObserver implements EventObserver {

    private final EventRepository eventRepository;
    private final CommunicationService communicationService;
    private final List<CommunicationStrategy> strategies;

    /**
     * Creates the observer. Throws IllegalArgumentException if no strategy is given.
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
        // kept short so it fits in one SMS
        String body = event.getName() + ": " + change + ". - Ripple";
        for (Attendee attendee : eventRepository.findAttendees(event.getId())) {
            communicationService.setStrategy(strategyFor(attendee));
            communicationService.send(attendee, "Event update: " + event.getName(), body);
        }
    }

    /** First strategy with a valid address for the attendee, or the first one. */
    private CommunicationStrategy strategyFor(Attendee attendee) {
        for (CommunicationStrategy strategy : strategies) {
            if (strategy.isValidAddress(strategy.addressOf(attendee))) {
                return strategy;
            }
        }
        return strategies.get(0);
    }
}
