package com.ripple.repository;

import com.ripple.TestDatabase;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonorRepositoryTest {

    @TempDir
    Path tempDir;

    private DonorRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DonorRepository(TestDatabase.create(tempDir));
    }

    @Test
    void insertGivesIdAndDonorCanBeFoundAgain() {
        Donor saved = repository.insert(new Donor(0, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL));
        assertTrue(saved.getId() > 0);

        Donor found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Mona Ali", found.getName());
        assertEquals("mona@example.com", found.getEmail());
        assertEquals(ContactChannel.EMAIL, found.getPreferredChannel());
    }

    @Test
    void findAllIsSortedByNameAndCountMatches() {
        repository.insert(new Donor(0, "Zaid", "", "01012345678", ContactChannel.SMS));
        repository.insert(new Donor(0, "Amal", "amal@example.com", "", ContactChannel.EMAIL));

        assertEquals("Amal", repository.findAll().get(0).getName());
        assertEquals(2, repository.count());
    }

    @Test
    void unknownIdGivesEmpty() {
        assertTrue(repository.findById(999).isEmpty());
    }
}
