package com.ripple.repository;

import com.ripple.model.LookupEntry;

import java.util.List;
import java.util.Optional;

/**
 * Common methods for lookup tables.
 */
public interface LookupRepository<T extends LookupEntry> {

    /** All entries sorted by name. */
    List<T> findAll();

    /** Finds an entry by id. */
    Optional<T> findById(int id);

    /** True if another entry has this name, ignoring case (CR1). */
    boolean existsByName(String name, int excludeId);

    /** Saves a new entry and sets its id. */
    T insert(T entry);

    /** Updates an entry. */
    void update(T entry);

    /** Deletes an entry. */
    void delete(int id);

    /** How many records use this entry (CR2). */
    int countUsages(int id);
}
