package com.ripple.controller;

import com.ripple.logging.ApplicationLogger;
import com.ripple.model.DonationType;
import com.ripple.model.EventCategory;
import com.ripple.model.LookupEntry;
import com.ripple.service.LookupService;
import com.ripple.service.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Lookup Management screen (and for screens that fill combo boxes
 * from lookups).
 *
 * <p>It turns the screen's input into lookup objects, passes them to the right
 * {@link LookupService}, and turns the outcome into a {@link Result} for the status bar.
 * It never throws to the view.
 */
public class LookupController {

    private final LookupService<DonationType> donationTypes;
    private final LookupService<EventCategory> eventCategories;

    /**
     * Creates the controller.
     *
     * @param donationTypes   service for the Donation Types table
     * @param eventCategories service for the Event Categories table
     */
    public LookupController(LookupService<DonationType> donationTypes, LookupService<EventCategory> eventCategories) {
        this.donationTypes = donationTypes;
        this.eventCategories = eventCategories;
    }

    /**
     * Returns all entries of one table, sorted by name.
     *
     * @param kind which table
     * @return the entries
     */
    public List<LookupEntry> list(LookupKind kind) {
        return switch (kind) {
            case DONATION_TYPE -> new ArrayList<>(donationTypes.findAll());
            case EVENT_CATEGORY -> new ArrayList<>(eventCategories.findAll());
        };
    }

    /**
     * Returns all donation types, e.g. for the Donations screen's combo box.
     *
     * @return the donation types sorted by name
     */
    public List<DonationType> listDonationTypes() {
        return donationTypes.findAll();
    }

    /**
     * Returns all event categories, e.g. for the Events screen's combo box.
     *
     * @return the event categories sorted by name
     */
    public List<EventCategory> listEventCategories() {
        return eventCategories.findAll();
    }

    /**
     * Adds a new entry ({@code id == null}) or updates an existing one.
     *
     * @param kind          which table
     * @param id            the entry's id, or {@code null} to add a new entry
     * @param name          the name typed by the user
     * @param description   the description typed by the user
     * @param taxDeductible the tax checkbox (ignored for event categories)
     * @return what happened, for the status bar
     */
    public Result save(LookupKind kind, Integer id, String name, String description, boolean taxDeductible) {
        boolean adding = id == null;
        int entryId = adding ? 0 : id;
        try {
            String label;
            LookupEntry entry;
            if (kind == LookupKind.DONATION_TYPE) {
                DonationType type = new DonationType(entryId, name, description, taxDeductible);
                if (adding) {
                    donationTypes.add(type);
                } else {
                    donationTypes.update(type);
                }
                label = "Donation type";
                entry = type;
            } else {
                EventCategory category = new EventCategory(entryId, name, description);
                if (adding) {
                    eventCategories.add(category);
                } else {
                    eventCategories.update(category);
                }
                label = "Event category";
                entry = category;
            }
            String message = label + " '" + entry.getName() + "' " + (adding ? "added" : "updated");
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not save because of a database error. Details are in logs/ripple.log.");
        }
    }

    /**
     * Deletes an entry, unless it is in use (CR2).
     *
     * @param kind which table
     * @param id   the entry's id
     * @return what happened, for the status bar
     */
    public Result delete(LookupKind kind, int id) {
        try {
            // Look the name up first, only so the message can say what was deleted.
            String name = list(kind).stream().filter(entry -> entry.getId() == id)
                    .map(LookupEntry::getName).findFirst().orElse("");
            String label;
            if (kind == LookupKind.DONATION_TYPE) {
                donationTypes.delete(id);
                label = "Donation type";
            } else {
                eventCategories.delete(id);
                label = "Event category";
            }
            String message = label + " '" + name + "' deleted";
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not delete because of a database error. Details are in logs/ripple.log.");
        }
    }
}
