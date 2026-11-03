package com.ripple.observer;

import com.ripple.TestDatabase;
import com.ripple.communication.CommunicationService;
import com.ripple.communication.EmailStrategy;
import com.ripple.communication.MessageLogEntry;
import com.ripple.communication.SmsStrategy;
import com.ripple.model.Attendee;
import com.ripple.model.ContactChannel;
import com.ripple.model.Event;
import com.ripple.model.EventCategory;
import com.ripple.repository.Database;
import com.ripple.repository.EventCategoryRepository;
import com.ripple.repository.EventRepository;
import com.ripple.repository.MessageLogRepository;
import com.ripple.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that attendees hear about a new date or location, by Email or else by SMS. */
class AttendeeNotificationObserverTest {

    @TempDir
    Path tempDir;

    private EventRepository events;
    private MessageLogRepository log;
    private EventService service;
    private Event event;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        events = new EventRepository(db);
        log = new MessageLogRepository(db);
        service = new EventService(events);
        service.registerObserver(new AttendeeNotificationObserver(events, new CommunicationService(log),
                List.of(new EmailStrategy(), new SmsStrategy())));
        EventCategory category = new EventCategoryRepository(db).findAll().get(0);
        event = service.addEvent(new Event(0, "Food drive", category, LocalDate.of(2026, 12, 15), "Cairo", ""));
    }

    private void moveTheEventToTheTwentieth() {
        service.updateEvent(new Event(event.getId(), event.getName(), event.getCategory(),
                LocalDate.of(2026, 12, 20), "Cairo", ""));
    }

    @Test
    void eachAttendeeIsToldByEmailOrElseBySms() {
        events.addAttendee(new Attendee(0, event.getId(), "Mona", "mona@example.com", "01012345678"));
        events.addAttendee(new Attendee(0, event.getId(), "Omar", "", "01198765432"));

        moveTheEventToTheTwentieth();

        List<MessageLogEntry> entries = log.findAll().stream()
                .sorted(Comparator.comparing(MessageLogEntry::recipient)).toList();
        assertEquals(2, entries.size());
        assertEquals(ContactChannel.EMAIL, entries.get(0).channel()); // Mona has an email
        assertEquals("Mona <mona@example.com>", entries.get(0).recipient());
        assertEquals(ContactChannel.SMS, entries.get(1).channel());   // Omar has only a phone
        assertEquals("Omar <01198765432>", entries.get(1).recipient());
        assertEquals("Event update: Food drive", entries.get(0).subject());
        assertTrue(entries.get(0).body().contains("date changed from 2026-12-15 to 2026-12-20"));
        assertTrue(entries.get(1).body().length() <= SmsStrategy.MAX_LENGTH); // fits in one SMS
        assertEquals("SIMULATED", entries.get(1).status());
    }

    @Test
    void anAttendeeWithNoAddressIsLoggedAsFailedNotSkipped() {
        events.addAttendee(new Attendee(0, event.getId(), "Sara", "", ""));

        moveTheEventToTheTwentieth();

        MessageLogEntry entry = log.findAll().get(0);
        assertEquals(ContactChannel.EMAIL, entry.channel());
        assertEquals("FAILED: no valid email address", entry.status());
    }

    @Test
    void onlyTheChangedEventsAttendeesAreTold() {
        Event other = service.addEvent(new Event(0, "Workshop", event.getCategory(), LocalDate.of(2026, 11, 1),
                "Giza", ""));
        events.addAttendee(new Attendee(0, other.getId(), "Hana", "hana@example.com", ""));

        moveTheEventToTheTwentieth(); // the food drive has no attendees

        assertTrue(log.findAll().isEmpty());
    }

    @Test
    void atLeastOneStrategyIsRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> new AttendeeNotificationObserver(events, new CommunicationService(log), List.of()));
    }
}
