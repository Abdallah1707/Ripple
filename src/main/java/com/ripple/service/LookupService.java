package com.ripple.service;

import com.ripple.model.LookupEntry;
import com.ripple.repository.LookupRepository;

import java.util.List;

/**
 * The rules for lookup tables, written once and used for both Donation Types and
 * Event Categories (one instance each):
 * <ul>
 *   <li>CR1 — names are required, trimmed, 2–50 characters and unique ignoring case;</li>
 *   <li>CR2 — an entry that is used by other records cannot be deleted.</li>
 * </ul>
 *
 * @param <T> the lookup entity, {@code DonationType} or {@code EventCategory}
 */
public class LookupService<T extends LookupEntry> {

    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final LookupRepository<T> repository;
    private final String entityLabel;

    /**
     * Creates the service.
     *
     * @param repository  the table to manage
     * @param entityLabel how one entry is called in messages, e.g. {@code "Donation type"}
     */
    public LookupService(LookupRepository<T> repository, String entityLabel) {
        this.repository = repository;
        this.entityLabel = entityLabel;
    }

    /**
     * Returns all entries sorted by name.
     *
     * @return all entries
     */
    public List<T> findAll() {
        return repository.findAll();
    }

    /**
     * Validates and saves a new entry.
     *
     * @param entry the new entry
     * @return the saved entry with its id
     * @throws ValidationException if the name is invalid or already used
     */
    public T add(T entry) {
        validate(entry);
        return repository.insert(entry);
    }

    /**
     * Validates and saves changes to an existing entry.
     *
     * @param entry the entry with its id and new values
     * @throws ValidationException if the entry does not exist, or the name is invalid or already used
     */
    public void update(T entry) {
        if (repository.findById(entry.getId()).isEmpty()) {
            throw new ValidationException(entityLabel + " not found");
        }
        validate(entry);
        repository.update(entry);
    }

    /**
     * Deletes an entry if nothing uses it (CR2).
     *
     * @param id the entry's id
     * @throws ValidationException if the entry does not exist or is in use
     */
    public void delete(int id) {
        T entry = repository.findById(id)
                .orElseThrow(() -> new ValidationException(entityLabel + " not found"));
        int usages = repository.countUsages(id);
        if (usages > 0) {
            throw new ValidationException("'" + entry.getName() + "' is used by " + usages
                    + " record(s) and cannot be deleted");
        }
        repository.delete(id);
    }

    /** Applies CR1 and stores the cleaned-up (trimmed) values back into the entry. */
    private void validate(T entry) {
        String name = InputValidator.requireText(entry.getName(), "Name", 2, 50);
        String description = entry.getDescription() == null ? "" : entry.getDescription().trim();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (repository.existsByName(name, entry.getId())) {
            throw new ValidationException(entityLabel + " '" + name + "' already exists");
        }
        entry.setName(name);
        entry.setDescription(description);
    }
}
