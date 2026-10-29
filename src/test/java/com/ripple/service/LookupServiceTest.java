package com.ripple.service;

import com.ripple.TestDatabase;
import com.ripple.model.DonationType;
import com.ripple.model.EventCategory;
import com.ripple.repository.Database;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.EventCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookupServiceTest {

    @TempDir
    Path tempDir;

    private Database db;
    private LookupService<DonationType> types;
    private LookupService<EventCategory> categories;

    @BeforeEach
    void setUp() {
        db = TestDatabase.create(tempDir);
        types = new LookupService<>(new DonationTypeRepository(db), "Donation type");
        categories = new LookupService<>(new EventCategoryRepository(db), "Event category");
    }

    private DonationType findType(String name) {
        return types.findAll().stream().filter(t -> t.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void addValidEntryTrimsAndSavesIt() {
        DonationType saved = types.add(new DonationType(0, "  Emergency Relief ", " Floods ", true));
        assertTrue(saved.getId() > 0);
        DonationType reloaded = findType("Emergency Relief");
        assertEquals("Floods", reloaded.getDescription());
        assertTrue(reloaded.isTaxDeductible());
    }

    @Test
    void rejectsBlankTooShortAndTooLongNames() {
        assertEquals("Name is required", assertThrows(ValidationException.class,
                () -> types.add(new DonationType(0, "  ", "", false))).getMessage());
        assertThrows(ValidationException.class, () -> types.add(new DonationType(0, "Z", "", false)));
        assertThrows(ValidationException.class, () -> types.add(new DonationType(0, "x".repeat(51), "", false)));
    }

    @Test
    void rejectsDuplicateNameIgnoringCaseAndSpaces() { // CR1
        ValidationException e = assertThrows(ValidationException.class,
                () -> types.add(new DonationType(0, "zakat ", "", true)));
        assertEquals("Donation type 'zakat' already exists", e.getMessage());
    }

    @Test
    void renamingToAnotherEntrysNameIsRejected() {
        DonationType education = findType("Education");
        education.setName("ZAKAT");
        assertThrows(ValidationException.class, () -> types.update(education));
    }

    @Test
    void savingAnEntryWithItsOwnNameIsAllowed() {
        DonationType zakat = findType("Zakat");
        zakat.setDescription("Updated description");
        types.update(zakat);
        assertEquals("Updated description", findType("Zakat").getDescription());
    }

    @Test
    void deleteUnusedEntryWorks() {
        EventCategory workshop = categories.findAll().stream()
                .filter(c -> c.getName().equals("Workshop")).findFirst().orElseThrow();
        categories.delete(workshop.getId());
        assertFalse(categories.findAll().stream().anyMatch(c -> c.getName().equals("Workshop")));
    }

    @Test
    void deleteEntryInUseIsBlockedWithCount() throws SQLException { // CR2
        DonationType zakat = findType("Zakat");
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO donors (name, preferred_channel) VALUES ('Mona', 'EMAIL')");
            for (int i = 0; i < 2; i++) {
                s.executeUpdate("INSERT INTO donations (donor_id, donation_type_id, method, amount, created_at) "
                        + "VALUES (1, " + zakat.getId() + ", 'CASH', 100, '2026-01-01T10:00')");
            }
        }
        ValidationException e = assertThrows(ValidationException.class, () -> types.delete(zakat.getId()));
        assertEquals("'Zakat' is used by 2 record(s) and cannot be deleted", e.getMessage());
        assertTrue(types.findAll().stream().anyMatch(t -> t.getName().equals("Zakat")));
    }

    @Test
    void updatingOrDeletingMissingEntryIsRejected() {
        assertEquals("Donation type not found", assertThrows(ValidationException.class,
                () -> types.delete(999)).getMessage());
        assertThrows(ValidationException.class, () -> types.update(new DonationType(999, "Ghost", "", false)));
    }
}
