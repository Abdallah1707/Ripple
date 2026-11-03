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
 * Controller for the Events screen (and for screens that need the list of events).
 *
 * <p>Checks the typed values (a name and a category are required, the date must look like
 * {@code 2026-12-15}), saves events through {@link EventService} (which notifies attendees when
 * the date or location changes), reads the rest from the repositories, and turns the outcome
 * into a {@link Result} for the status bar. It never throws to the view.
 */
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final VolunteerRepository volunteerRepository;

    /**
     * Creates the controller.
     *
     * @param eventService        saves events and is the Observer subject for event changes
     * @param eventRepository     where events and attendees are read from (and deleted)
     * @param volunteerRepository where the volunteers assigned to an event are read from
     */
    public EventController(EventService eventService, EventRepository eventRepository,
                           VolunteerRepository volunteerRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.volunteerRepository = volunteerRepository;
    }

    /**
     * Returns all events, soonest first.
     *
     * @return the events
     */
    public List<Event> listEvents() {
        return eventRepository.findAll();
    }

    /**
     * Adds a new event ({@code id == null}) or updates an existing one.
     *
     * @param id          the event's id, or {@code null} to add a new one
     * @param name        the name typed by the user (required)
     * @param categoryId  id of the chosen event category (required, {@code <= 0} means none chosen)
     * @param date        the date typed by the user, as {@code yyyy-MM-dd}, e.g. {@code 2026-12-15}
     * @param location    the location typed by the user
     * @param description the description typed by the user
     * @return what happened, for the status bar
     */
    public Result saveEvent(Integer id, String name, int categoryId, String date, String location,
                            String description) {
        boolean adding = id == null;
        try {
            String cleanName = InputValidator.requireText(name, "Event name", 1, 100);
            if (categoryId <= 0) {
                throw new ValidationException("Please choose an event category");
            }
            LocalDate eventDate = parseDate(date);
            // Only the id matters for saving; the repository reads the full category back when it loads events.
            Event event = new Event(adding ? 0 : id, cleanName, new EventCategory(categoryId, "", ""), eventDate,
                    clean(location), clean(description));
            String message = "Event '" + cleanName + "' " + (adding ? "added" : "updated");
            if (adding) {
                eventService.addEvent(event);
            } else if (eventService.updateEvent(event)) { // true when the date or location changed
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

    /**
     * Deletes an event together with its attendees and volunteer assignments.
     *
     * @param id the event's id
     * @return what happened, for the status bar
     */
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

    /**
     * Registers an attendee for an event.
     *
     * @param eventId the event's id
     * @param name    the name typed by the user (required)
     * @param email   the email typed by the user (optional, must be valid if filled in)
     * @param phone   the phone typed by the user (optional, must be valid if filled in)
     * @return what happened, for the status bar
     */
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

    /**
     * Returns the attendees of an event.
     *
     * @param eventId the event's id
     * @return the attendees sorted by name
     */
    public List<Attendee> listAttendees(int eventId) {
        return eventRepository.findAttendees(eventId);
    }

    /**
     * Returns the volunteers assigned to an event.
     *
     * @param eventId the event's id
     * @return the assigned volunteers sorted by name
     */
    public List<Volunteer> listVolunteers(int eventId) {
        return volunteerRepository.findByEvent(eventId);
    }

    private static LocalDate parseDate(String text) {
        String trimmed = InputValidator.requireText(text, "Date", 1, 20);
        try {
            return LocalDate.parse(trimmed); // ISO format yyyy-MM-dd; rejects 2026-02-30 etc.
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date must look like 2026-12-15 (year-month-day)");
        }
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }
}
