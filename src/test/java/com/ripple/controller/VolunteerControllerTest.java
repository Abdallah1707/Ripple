package com.ripple.controller;

import com.ripple.TestDatabase;
import com.ripple.model.Event;
import com.ripple.model.Volunteer;
import com.ripple.repository.Database;
import com.ripple.repository.EventCategoryRepository;
import com.ripple.repository.EventRepository;
import com.ripple.repository.VolunteerRepository;
import com.ripple.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the Volunteers screen's rules: validation, add / edit / delete, and event assignment. */
class VolunteerControllerTest {

    @TempDir
    Path tempDir;

    private VolunteerController controller;
    private VolunteerRepository volunteers;
    private EventController events;
    private int eventId;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        volunteers = new VolunteerRepository(db);
        EventRepository eventRepository = new EventRepository(db);
        controller = new VolunteerController(volunteers);
        events = new EventController(new EventService(eventRepository), eventRepository, volunteers);
        int categoryId = new EventCategoryRepository(db).findAll().get(0).getId();
        assertTrue(events.saveEvent(null, "Food drive", categoryId, "2026-12-15", "Cairo", "").success());
        eventId = events.listEvents().get(0).getId();
    }

    @Test
    void addEditAndDeleteWork() {
        assertTrue(controller.saveVolunteer(null, "Sara Hassan", "sara@example.com", "01012345678",
                "First aid", "Weekends").success());
        Volunteer saved = controller.listVolunteers().get(0);
        assertEquals("First aid", saved.getSkills());

        Result updated = controller.saveVolunteer(saved.getId(), "Sara H.", "", "", "Driving", "Evenings");
        assertTrue(updated.success());
        Volunteer reloaded = controller.listVolunteers().get(0);
        assertEquals("Sara H.", reloaded.getName());
        assertEquals("Driving", reloaded.getSkills());
        assertEquals(1, controller.listVolunteers().size()); // updated, not duplicated

        assertTrue(controller.deleteVolunteer(saved.getId()).success());
        assertTrue(controller.listVolunteers().isEmpty());
    }

    @Test
    void nameIsRequiredAndNothingIsSaved() {
        Result result = controller.saveVolunteer(null, "   ", "", "", "", "");
        assertFalse(result.success());
        assertEquals("Name is required", result.message());
        assertTrue(controller.listVolunteers().isEmpty());
    }

    @Test
    void emailAndPhoneAreCheckedOnlyWhenFilledIn() {
        assertFalse(controller.saveVolunteer(null, "Omar", "not-an-email", "", "", "").success());
        assertFalse(controller.saveVolunteer(null, "Omar", "", "12ab", "", "").success());
        assertTrue(controller.saveVolunteer(null, "Omar", "", "", "", "").success()); // both empty is fine
        assertEquals(1, controller.listVolunteers().size());
    }

    @Test
    void assigningTheSameVolunteerTwiceDoesNotCreateADuplicate() {
        controller.saveVolunteer(null, "Sara Hassan", "", "", "", "");
        int volunteerId = controller.listVolunteers().get(0).getId();

        assertTrue(controller.assignToEvent(volunteerId, eventId).success());
        Result second = controller.assignToEvent(volunteerId, eventId);

        assertFalse(second.success());
        assertEquals(1, events.listVolunteers(eventId).size());
        // Even if the repository is called directly, the primary key keeps it to one row.
        volunteers.assignToEvent(volunteerId, eventId);
        assertEquals(1, events.listVolunteers(eventId).size());
    }

    @Test
    void assignmentNeedsAVolunteerAndAnEvent() {
        assertFalse(controller.assignToEvent(0, eventId).success());
        assertFalse(controller.assignToEvent(1, 0).success());
    }

    @Test
    void deletingAVolunteerRemovesTheirAssignments() {
        controller.saveVolunteer(null, "Sara Hassan", "", "", "", "");
        int volunteerId = controller.listVolunteers().get(0).getId();
        controller.assignToEvent(volunteerId, eventId);

        controller.deleteVolunteer(volunteerId);

        assertTrue(events.listVolunteers(eventId).isEmpty());
        Event stillThere = events.listEvents().get(0);
        assertEquals("Food drive", stillThere.getName()); // the event itself is untouched
    }
}
