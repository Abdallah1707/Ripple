package com.ripple.controller;

import com.ripple.logging.ApplicationLogger;
import com.ripple.model.Attendee;
import com.ripple.model.Event;
import com.ripple.model.EventCategory;
import com.ripple.model.Volunteer;
import com.ripple.repository.EventRepository;
import com.ripple.repository.VolunteerRepository;
import com.ripple.service.EventService;
import com.ripple.service.InputValidator;
import com.ripple.service.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controller for the Events screen.
 */
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final VolunteerRepository volunteerRepository;

    /** Creates the controller. */
    public EventController(EventService eventService, EventRepository eventRepository,
                           VolunteerRepository volunteerRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.volunteerRepository = volunteerRepository;
    }

    /** All events, soonest first. */
    public List<Event> listEvents() {
        return eventRepository.findAll();
    }

    /** Adds an event if id is null, otherwise updates it. Date is yyyy-MM-dd. */
    public Result saveEvent(Integer id, String name, int categoryId, String date, String location,
                            String description) {
        boolean adding = id == null;
        try {
            String cleanName = InputValidator.requireText(name, "Event name", 1, 100);
            if (categoryId <= 0) {
                throw new ValidationException("Please choose an event category");
            }
            LocalDate eventDate = parseDate(date);
            // only the category id is needed to save
            Event event = new Event(adding ? 0 : id, cleanName, new EventCategory(categoryId, "", ""), eventDate,
                    clean(location), clean(description));
            String message = "Event '" + cleanName + "' " + (adding ? "added" : "updated");
            if (adding) {
                eventService.addEvent(event);
            } else if (eventService.updateEvent(event)) { // date or location changed
                message += "; attendees were notified of the change";
            }
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not save the event because of a database error. "
                    + "Details are in logs/ripple.log.");
        }
    }

    /** Deletes an event with its attendees and volunteers. */
    public Result deleteEvent(int id) {
        try {
            String name = eventRepository.findById(id).map(Event::getName).orElse("");
            eventRepository.delete(id);
            String message = "Event '" + name + "' deleted";
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not delete the event because of a database error. "
                    + "Details are in logs/ripple.log.");
        }
    }

    /** Registers an attendee for an event. */
    public Result registerAttendee(int eventId, String name, String email, String phone) {
        try {
            if (eventId <= 0) {
                throw new ValidationException("Please select an event first");
            }
            String cleanName = InputValidator.requireText(name, "Attendee name", 1, 100);
            Attendee attendee = new Attendee(0, eventId, cleanName, InputValidator.optionalEmail(email),
                    InputValidator.optionalPhone(phone));
            eventRepository.addAttendee(attendee);
            String message = "Attendee '" + cleanName + "' registered";
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not register the attendee because of a database error. "
                    + "Details are in logs/ripple.log.");
        }
    }

    /** Attendees of an event. */
    public List<Attendee> listAttendees(int eventId) {
        return eventRepository.findAttendees(eventId);
    }

    /** Volunteers assigned to an event. */
    public List<Volunteer> listVolunteers(int eventId) {
        return volunteerRepository.findByEvent(eventId);
    }

    private static LocalDate parseDate(String text) {
        String trimmed = InputValidator.requireText(text, "Date", 1, 20);
        try {
            return LocalDate.parse(trimmed); // yyyy-MM-dd
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date must look like 2026-12-15 (year-month-day)");
        }
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }
}
