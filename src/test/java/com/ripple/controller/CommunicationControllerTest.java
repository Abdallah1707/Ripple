package com.ripple.controller;

import com.ripple.TestDatabase;
import com.ripple.communication.CommunicationService;
import com.ripple.communication.EmailStrategy;
import com.ripple.communication.MessageLogEntry;
import com.ripple.communication.SmsStrategy;
import com.ripple.model.Attendee;
import com.ripple.model.Beneficiary;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donor;
import com.ripple.model.Event;
import com.ripple.model.Volunteer;
import com.ripple.repository.BeneficiaryRepository;
import com.ripple.repository.Database;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.EventCategoryRepository;
import com.ripple.repository.EventRepository;
import com.ripple.repository.MessageLogRepository;
import com.ripple.repository.VolunteerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests sending to each group on both channels, and the messages shown in the status bar. */
class CommunicationControllerTest {

    @TempDir
    Path tempDir;

    private CommunicationController controller;
    private EventRepository events;
    private int eventId;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        DonorRepository donors = new DonorRepository(db);
        VolunteerRepository volunteers = new VolunteerRepository(db);
        BeneficiaryRepository beneficiaries = new BeneficiaryRepository(db);
        events = new EventRepository(db);
        MessageLogRepository log = new MessageLogRepository(db);
        controller = new CommunicationController(new CommunicationService(log),
                Map.of(ContactChannel.EMAIL, new EmailStrategy(), ContactChannel.SMS, new SmsStrategy()),
                donors, volunteers, beneficiaries, events, log);

        // Two donors: one with an email only, one with a phone only.
        donors.insert(new Donor(0, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL));
        donors.insert(new Donor(0, "Omar Samy", "", "01012345678", ContactChannel.SMS));
        volunteers.insert(new Volunteer(0, "Sara", "sara@example.com", "01198765432", "", ""));
        beneficiaries.insert(new Beneficiary(0, "Hassan", "", "01234567890", "", ""));
        Event event = events.insert(new Event(0, "Winter drive",
                new EventCategoryRepository(db).findAll().get(0), LocalDate.of(2026, 12, 15), "Cairo", ""));
        eventId = event.getId();
        events.addAttendee(new Attendee(0, eventId, "Laila", "laila@example.com", ""));
    }

    @Test
    void sendingToDonorsByEmailSkipsTheOneWithoutAnEmail() {
        Result result = controller.sendToGroup(Audience.DONORS, null, "EMAIL", "Update", "Thanks for your support");

        assertTrue(result.success());
        assertEquals("Sent 1, skipped 1 (no valid email address)", result.message());
        assertEquals(2, controller.listMessageLog().size());
    }

    @Test
    void sendingTheSameMessageBySmsUsesPhoneNumbersInstead() {
        controller.sendToGroup(Audience.DONORS, null, "SMS", "Update", "Thanks for your support");

        List<MessageLogEntry> log = controller.listMessageLog();
        assertTrue(log.stream().allMatch(e -> e.channel() == ContactChannel.SMS));
        assertTrue(log.stream().anyMatch(e -> e.recipient().equals("Omar Samy <01012345678>")
                && e.status().equals("SIMULATED")));
        assertTrue(log.stream().anyMatch(e -> e.recipient().equals("Mona Ali")
                && e.status().equals("FAILED: no valid phone number")));
    }

    @Test
    void everyGroupCanBeMessagedOnBothChannels() {
        for (String channel : List.of("EMAIL", "SMS")) {
            assertTrue(controller.sendToGroup(Audience.VOLUNTEERS, null, channel, "Hi", "Hello").success());
        }
        assertTrue(controller.sendToGroup(Audience.BENEFICIARIES, null, "SMS", "Hi", "Hello").success());
        assertFalse(controller.sendToGroup(Audience.BENEFICIARIES, null, "EMAIL", "Hi", "Hello").success());
        assertTrue(controller.sendToGroup(Audience.EVENT_ATTENDEES, eventId, "EMAIL", "Hi", "See you").success());
        assertEquals(5, controller.listMessageLog().size());
    }

    @Test
    void eventAttendeesNeedAnEvent() {
        Result result = controller.sendToGroup(Audience.EVENT_ATTENDEES, null, "EMAIL", "Hi", "Hello");
        assertFalse(result.success());
        assertEquals("Choose an event to message its attendees", result.message());
    }

    @Test
    void badInputGivesRedResultAndLogsNothing() {
        assertFalse(controller.sendToGroup(Audience.DONORS, null, "EMAIL", "", "Hello").success());
        assertFalse(controller.sendToGroup(Audience.DONORS, null, "EMAIL", "Hi", "  ").success());
        assertFalse(controller.sendToGroup(Audience.DONORS, null, "FAX", "Hi", "Hello").success());
        assertFalse(controller.sendToGroup(null, null, "EMAIL", "Hi", "Hello").success());
        assertTrue(controller.listMessageLog().isEmpty());
    }

    @Test
    void anSmsOver160CharactersIsSkippedForEveryone() {
        Result result = controller.sendToGroup(Audience.VOLUNTEERS, null, "SMS", "Hi", "x".repeat(161));
        assertFalse(result.success());
        assertEquals("Sent 0, skipped 1 (message is longer than 160 characters)", result.message());
    }
}
