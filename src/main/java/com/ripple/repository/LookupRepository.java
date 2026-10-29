package com.ripple.repository;

import com.ripple.model.LookupEntry;

import java.util.List;
import java.util.Optional;

/**
 * What every lookup table can do. Both lookup tables implement it, which lets one
 * {@code LookupService} apply the same rules to both.
 *
 * @param <T> the lookup entity, {@code DonationType} or {@code EventCategory}
 */
public interface LookupRepository<T extends LookupEntry> {

    /**
     * Returns all entries sorted by name.
     *
     * @return all entries
     */
    List<T> findAll();

    /**
     * Finds one entry.
     *
     * @param id the entry's id
     * @return the entry, or empty if there is no entry with that id
     */
    Optional<T> findById(int id);

    /**
     * Tells whether another entry already uses this name, ignoring case (CR1).
     *
     * @param name      the name to look for
     * @param excludeId an id to ignore (the entry being renamed), or 0 when adding
     * @return {@code true} if a different entry has this name
     */
    boolean existsByName(String name, int excludeId);

    /**
     * Saves a new entry and sets its generated id.
     *
     * @param entry the entry to save (its id is ignored)
     * @return the same entry, now with its id
     */
    T insert(T entry);

    /**
     * Saves changes to an existing entry.
     *
     * @param entry the entry with its id and new values
     */
    void update(T entry);

    /**
     * Deletes an entry.
     *
     * @param id the entry's id
     */
    void delete(int id);

    /**
     * Counts the records that use this entry, e.g. donations of this type (CR2).
     *
     * @param id the entry's id
     * @return how many records reference it
     */
    int countUsages(int id);
}
