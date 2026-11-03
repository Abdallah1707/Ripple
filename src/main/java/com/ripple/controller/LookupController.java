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
 * Controller for the Lookup Management screen.
 */
public class LookupController {

    private final LookupService<DonationType> donationTypes;
    private final LookupService<EventCategory> eventCategories;

    /** Creates the controller. */
    public LookupController(LookupService<DonationType> donationTypes, LookupService<EventCategory> eventCategories) {
        this.donationTypes = donationTypes;
        this.eventCategories = eventCategories;
    }

    /** All entries of a table, sorted by name. */
    public List<LookupEntry> list(LookupKind kind) {
        return switch (kind) {
            case DONATION_TYPE -> new ArrayList<>(donationTypes.findAll());
            case EVENT_CATEGORY -> new ArrayList<>(eventCategories.findAll());
        };
    }

    /** All donation types. */
    public List<DonationType> listDonationTypes() {
        return donationTypes.findAll();
    }

    /** All event categories. */
    public List<EventCategory> listEventCategories() {
        return eventCategories.findAll();
    }

    /** Adds an entry if id is null, otherwise updates it. */
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

    /** Deletes an entry unless it is in use (CR2). */
    public Result delete(LookupKind kind, int id) {
        try {
            // get the name for the message
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
