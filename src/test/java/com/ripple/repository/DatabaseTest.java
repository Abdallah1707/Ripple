package com.ripple.repository;

import com.ripple.TestDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseTest {

    @TempDir
    Path tempDir;

    @Test
    void initializeCreatesAllElevenTables() throws SQLException {
        Database db = TestDatabase.create(tempDir);
        assertEquals(11, count(db, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' "
                + "AND name NOT LIKE 'sqlite_%'"));
    }

    @Test
    void initializeSeedsLookupsOnlyOnce() throws SQLException {
        Database db = TestDatabase.create(tempDir);
        db.initialize(); // second start of the app
        assertEquals(4, count(db, "SELECT COUNT(*) FROM donation_types"));
        assertEquals(3, count(db, "SELECT COUNT(*) FROM event_categories"));
        assertEquals(1, count(db, "SELECT COUNT(*) FROM donation_types WHERE name = 'Food Supplies' "
                + "AND tax_deductible = 0"));
    }

    @Test
    void foreignKeysAreSwitchedOn() throws SQLException {
        Database db = TestDatabase.create(tempDir);
        assertEquals(1, count(db, "PRAGMA foreign_keys"));
        // A donation pointing at a donor that does not exist must be rejected.
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO donations (donor_id, donation_type_id, method, created_at) "
                            + "VALUES (999, 1, 'CASH', '2026-01-01T10:00')"));
        }
    }

    @Test
    void lookupNamesAreUniqueIgnoringCase() throws SQLException {
        Database db = TestDatabase.create(tempDir);
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO donation_types (name) VALUES ('ZAKAT')"));
        }
    }

    private static int count(Database db, String sql) throws SQLException {
        try (Connection c = db.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            return r.getInt(1);
        }
    }
}
