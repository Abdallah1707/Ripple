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
    void initializeAddsTheSocialHandleColumnToAnOlderDatabase() throws SQLException {
        Database db = new Database(tempDir.resolve("old.db").toString());
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            // the donors table as it was before social media was added
            s.execute("CREATE TABLE donors (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, "
                    + "email TEXT, phone TEXT, preferred_channel TEXT NOT NULL)");
            s.execute("INSERT INTO donors (name, email, preferred_channel) VALUES ('Mona', 'm@x.com', 'EMAIL')");
        }

        db.initialize();
        db.initialize(); // a second start must not try to add the column again

        assertEquals(1, count(db, "SELECT COUNT(*) FROM pragma_table_info('donors') WHERE name = 'social_handle'"));
        assertEquals(1, count(db, "SELECT COUNT(*) FROM donors WHERE name = 'Mona'")); // old rows are kept
    }

    @Test
    void initializeAddsTheDonationExtrasColumnsToAnOlderDatabase() throws SQLException {
        Database db = new Database(tempDir.resolve("old-donations.db").toString());
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            // the donations table as it was before urgent / gift / anonymous were added
            s.execute("CREATE TABLE donations (id INTEGER PRIMARY KEY AUTOINCREMENT, donor_id INTEGER NOT NULL, "
                    + "donation_type_id INTEGER NOT NULL, method TEXT NOT NULL, amount REAL, transaction_ref TEXT, "
                    + "item_description TEXT, quantity INTEGER, estimated_value REAL, created_at TEXT NOT NULL)");
        }

        db.initialize();
        db.initialize(); // a second start must not add them again

        assertEquals(3, count(db, "SELECT COUNT(*) FROM pragma_table_info('donations') "
                + "WHERE name IN ('urgent', 'gift_honoree', 'anonymous')"));
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
