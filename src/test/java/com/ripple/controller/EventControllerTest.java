package com.ripple.controller;

import com.ripple.TestDatabase;
import com.ripple.model.Attendee;
import com.ripple.model.Event;
import com.ripple.model.EventCategory;
import com.ripple.repository.Database;
import com.ripple.repository.EventCategoryRepository;
import com.ripple.repository.EventRepository;
import com.ripple.repository.VolunteerRepository;
import com.ripple.service.LookupService;
import com.ripple.service.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the Events screen's rules: validation, add / edit / delete, attendees, and the category link. */
class EventControllerTest {

    @TempDir
    Path tempDir;

    private EventController controller;
    private VolunteerController volunteerController;
    private LookupService<EventCategory> categories;
    private int categoryId;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        VolunteerRepository volunteers = new VolunteerRepository(db);
        controller = new EventController(new EventRepository(db), volunteers);
        volunteerController = new VolunteerController(volunteers);
        categories = new LookupService<>(new EventCategoryRepository(db), "Event category");
        categoryId = categories.findAll().stream().filter(c -> c.getName().equals("Outreach"))
                .findFirst().orElseThrow().getId();
    }

    @Test
    void addEditAndDeleteWork() {
        assertTrue(controller.saveEvent(null, "Winter clothes drive", categoryId, "2026-12-15", "Cairo",
                "Collecting coats").success());
        Event saved = controller.listEvents().get(0);
        assertEquals(LocalDate.of(2026, 12, 15), saved.getEventDate());
        assertEquals("Outreach", saved.getCategory().getName()); // category is loaded with the event

        assertTrue(controller.saveEvent(saved.getId(), "Winter coats", categoryId, "2026-12-20", "Giza", "")
                .success());
        Event reloaded = controller.listEvents().get(0);
        assertEquals("Winter coats", reloaded.getName());
        assertEquals(LocalDate.of(2026, 12, 20), reloaded.getEventDate());
        assertEquals(1, controller.listEvents().size());

        assertTrue(controller.deleteEvent(saved.getId()).success());
        assertTrue(controller.listEvents().isEmpty());
    }

    @Test
    void nameCategoryAndDateAreRequiredAndNothingIsSaved() {
        assertEquals("Event name is required",
                controller.saveEvent(null, " ", categoryId, "2026-12-15", "", "").message());
        assertEquals("Please choose an event category",
                controller.saveEvent(null, "Gala", 0, "2026-12-15", "", "").message());
        assertFalse(controller.saveEvent(null, "Gala", categoryId, "", "", "").success());
        assertTrue(controller.listEvents().isEmpty());
    }

    @Test
    void dateMustBeARealDayInYearMonthDayFormat() {
        assertFalse(controller.saveEvent(null, "Gala", categoryId, "15/12/2026", "", "").success());
        assertFalse(controller.saveEvent(null, "Gala", categoryId, "2026-02-30", "", "").success());
        assertEquals("Date must look like 2026-12-15 (year-month-day)",
                controller.saveEvent(null, "Gala", categoryId, "soon", "", "").message());
        assertTrue(controller.saveEvent(null, "Gala", categoryId, " 2026-12-15 ", "", "").success());
    }

    @Test
    void attendeesAreRegisteredPerEvent() {
        controller.saveEvent(null, "Workshop A", categoryId, "2026-11-01", "", "");
        controller.saveEvent(null, "Workshop B", categoryId, "2026-11-02", "", "");
        int a = controller.listEvents().get(0).getId();
        int b = controller.listEvents().get(1).getId();

        assertTrue(controller.registerAttendee(a, "Mona Ali", "mona@example.com", "").success());
        assertTrue(controller.registerAttendee(a, "Karim Saad", "", "01012345678").success());

        assertEquals(2, controller.listAttendees(a).size());
        assertTrue(controller.listAttendees(b).isEmpty());
        Attendee first = controller.listAttendees(a).get(0);
        assertEquals("Karim Saad", first.getName()); // sorted by name
        assertEquals(a, first.getEventId());
    }

    @Test
    void attendeeNeedsAnEventANameAndValidContactDetails() {
        controller.saveEvent(null, "Workshop", categoryId, "2026-11-01", "", "");
        int eventId = controller.listEvents().get(0).getId();

        assertFalse(controller.registerAttendee(0, "Mona", "", "").success());
        assertFalse(controller.registerAttendee(eventId, "", "", "").success());
        assertFalse(controller.registerAttendee(eventId, "Mona", "bad", "").success());
        assertFalse(controller.registerAttendee(eventId, "Mona", "", "12").success());
        assertTrue(controller.listAttendees(eventId).isEmpty());
    }

    @Test
    void deletingAnEventAlsoRemovesItsAttendeesAndAssignments() {
        controller.saveEvent(null, "Fundraiser night", categoryId, "2026-11-01", "", "");
        int eventId = controller.listEvents().get(0).getId();
        controller.registerAttendee(eventId, "Mona Ali", "", "");
        volunteerController.saveVolunteer(null, "Sara", "", "", "", "");
        volunteerController.assignToEvent(volunteerController.listVolunteers().get(0).getId(), eventId);
        assertEquals(1, controller.listAttendees(eventId).size());
        assertEquals(1, controller.listVolunteers(eventId).size());

        assertTrue(controller.deleteEvent(eventId).success());

        assertTrue(controller.listAttendees(eventId).isEmpty());
        assertTrue(controller.listVolunteers(eventId).isEmpty());
        assertEquals(1, volunteerController.listVolunteers().size()); // the volunteer is kept
    }

    @Test
    void anEventCategoryInUseCannotBeDeleted() { // CR2
        controller.saveEvent(null, "Beach clean-up", categoryId, "2026-11-01", "", "");

        ValidationException e = assertThrows(ValidationException.class, () -> categories.delete(categoryId));
        assertTrue(e.getMessage().contains("1"), e.getMessage()); // says how many events use it

        controller.deleteEvent(controller.listEvents().get(0).getId());
        categories.delete(categoryId); // free again once no event uses it
        assertTrue(categories.findAll().stream().noneMatch(c -> c.getId() == categoryId));
    }
}
