package com.ripple.service;

import com.ripple.model.LookupEntry;
import com.ripple.repository.LookupRepository;

import java.util.List;

/**
 * Rules for lookup tables, used for both donation types and event categories.
 * CR1: names are required, 2 to 50 chars and unique ignoring case.
 * CR2: entries in use can't be deleted.
 */
public class LookupService<T extends LookupEntry> {

    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final LookupRepository<T> repository;
    private final String entityLabel;

    /** Creates the service. */
    public LookupService(LookupRepository<T> repository, String entityLabel) {
        this.repository = repository;
        this.entityLabel = entityLabel;
    }

    /** All entries sorted by name. */
    public List<T> findAll() {
        return repository.findAll();
    }

    /** Checks and saves a new entry. */
    public T add(T entry) {
        validate(entry);
        return repository.insert(entry);
    }

    /** Checks and updates an entry. */
    public void update(T entry) {
        if (repository.findById(entry.getId()).isEmpty()) {
            throw new ValidationException(entityLabel + " not found");
        }
        validate(entry);
        repository.update(entry);
    }

    /** Deletes an entry if nothing uses it (CR2). */
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

    /** Checks CR1 and trims the values. */
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
