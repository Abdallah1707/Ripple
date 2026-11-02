package com.ripple.controller;

import com.ripple.logging.ApplicationLogger;
import com.ripple.model.Volunteer;
import com.ripple.repository.VolunteerRepository;
import com.ripple.service.InputValidator;
import com.ripple.service.ValidationException;

import java.util.List;

/**
 * Controller for the Volunteers screen.
 *
 * <p>Checks the typed values (a name is required; an email or phone must be valid if it is
 * filled in), calls {@link VolunteerRepository}, and turns the outcome into a
 * {@link Result} for the status bar. It never throws to the view.
 */
public class VolunteerController {

    private final VolunteerRepository volunteerRepository;

    /**
     * Creates the controller.
     *
     * @param volunteerRepository where volunteers are stored
     */
    public VolunteerController(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    /**
     * Returns all volunteers, e.g. for the table and the "assign" combo box.
     *
     * @return the volunteers sorted by name
     */
    public List<Volunteer> listVolunteers() {
        return volunteerRepository.findAll();
    }

    /**
     * Adds a new volunteer ({@code id == null}) or updates an existing one.
     *
     * @param id           the volunteer's id, or {@code null} to add a new one
     * @param name         the name typed by the user (required)
     * @param email        the email typed by the user (optional, must be valid if filled in)
     * @param phone        the phone typed by the user (optional, must be valid if filled in)
     * @param skills       the skills typed by the user
     * @param availability the availability typed by the user
     * @return what happened, for the status bar
     */
    public Result saveVolunteer(Integer id, String name, String email, String phone, String skills,
                                String availability) {
        boolean adding = id == null;
        try {
            String cleanName = InputValidator.requireText(name, "Name", 1, 100);
            Volunteer volunteer = new Volunteer(adding ? 0 : id, cleanName, InputValidator.optionalEmail(email),
                    InputValidator.optionalPhone(phone), clean(skills), clean(availability));
            if (adding) {
                volunteerRepository.insert(volunteer);
            } else {
                volunteerRepository.update(volunteer);
            }
            String message = "Volunteer '" + cleanName + "' " + (adding ? "added" : "updated");
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not save the volunteer because of a database error. "
                    + "Details are in logs/ripple.log.");
        }
    }

    /**
     * Deletes a volunteer. Their event assignments are removed with them.
     *
     * @param id the volunteer's id
     * @return what happened, for the status bar
     */
    public Result deleteVolunteer(int id) {
        try {
            String name = volunteerRepository.findAll().stream().filter(v -> v.getId() == id)
                    .map(Volunteer::getName).findFirst().orElse("");
            volunteerRepository.delete(id);
            String message = "Volunteer '" + name + "' deleted";
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not delete the volunteer because of a database error. "
                    + "Details are in logs/ripple.log.");
        }
    }

    /**
     * Assigns a volunteer to an event. Assigning the same volunteer to the same event twice
     * does not create a duplicate; the user is told they were already assigned.
     *
     * @param volunteerId the volunteer's id
     * @param eventId     the event's id
     * @return what happened, for the status bar
     */
    public Result assignToEvent(int volunteerId, int eventId) {
        if (volunteerId <= 0) {
            return Result.fail("Please choose a volunteer");
        }
        if (eventId <= 0) {
            return Result.fail("Please choose an event");
        }
        try {
            boolean alreadyAssigned = volunteerRepository.findByEvent(eventId).stream()
                    .anyMatch(v -> v.getId() == volunteerId);
            if (alreadyAssigned) {
                return Result.fail("This volunteer is already assigned to that event");
            }
            volunteerRepository.assignToEvent(volunteerId, eventId);
            String message = "Volunteer assigned to the event";
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not assign the volunteer because of a database error. "
                    + "Details are in logs/ripple.log.");
        }
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }
}
