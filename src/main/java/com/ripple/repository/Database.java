package com.ripple.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connects to the SQLite database and creates the tables on first run.
 * Not a singleton so each test can use its own database.
 */
public class Database {

    /** Default database file. */
    public static final String DEFAULT_PATH = "data/ripple.db";

    private final String url;

    /** Creates the database for a file. Makes the folder if it's missing. */
    public Database(String path) {
        Path file = Path.of(path).toAbsolutePath();
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create folder for database: " + file.getParent(), e);
        }
        this.url = "jdbc:sqlite:" + file;
    }

    /** Uses RIPPLE_DB_PATH if set, otherwise the default path. */
    public static Database fromEnvironment() {
        String path = System.getenv("RIPPLE_DB_PATH");
        return new Database(path == null || path.isBlank() ? DEFAULT_PATH : path);
    }

    /** Opens a connection with foreign keys turned on. */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /** Creates missing tables and adds starting data. Safe to call every time. */
    public void initialize() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : SCHEMA) {
                statement.execute(sql);
            }
            addColumnIfMissing(connection, "donors", "social_handle", "TEXT");
            addColumnIfMissing(connection, "donations", "urgent", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(connection, "donations", "gift_honoree", "TEXT");
            addColumnIfMissing(connection, "donations", "anonymous", "INTEGER NOT NULL DEFAULT 0");
            seedIfEmpty(connection, "donation_types",
                    "INSERT INTO donation_types (name, description, tax_deductible) VALUES (?, ?, ?)",
                    new Object[][]{
                            {"General Fund", "Unrestricted donations for any program", 1},
                            {"Zakat", "Obligatory charity distributed to eligible beneficiaries", 1},
                            {"Education", "School fees, books and tutoring", 1},
                            {"Food Supplies", "Food parcels and meals", 0}
                    });
            seedIfEmpty(connection, "event_categories",
                    "INSERT INTO event_categories (name, description) VALUES (?, ?)",
                    new Object[][]{
                            {"Fundraiser", "Events that raise money"},
                            {"Workshop", "Training and awareness sessions"},
                            {"Outreach", "Field visits and community activities"}
                    });
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Adds a column to an older database that doesn't have it yet. */
    private void addColumnIfMissing(Connection connection, String table, String column, String type)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (columns.getString("name").equalsIgnoreCase(column)) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

    /** Inserts the rows only if the table is empty. */
    private void seedIfEmpty(Connection connection, String table, String insertSql, Object[][] rows)
            throws SQLException {
        try (Statement count = connection.createStatement();
             ResultSet result = count.executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (result.getInt(1) > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    insert.setObject(i + 1, row[i]);
                }
                insert.executeUpdate();
            }
        }
    }

    /**
     * All tables. Lookup names are unique ignoring case (CR1).
     * No cascade on lookups so used ones can't be deleted (CR2).
     */
    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS donation_types (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                name           TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                description    TEXT    NOT NULL DEFAULT '',
                tax_deductible INTEGER NOT NULL DEFAULT 0
            )""",
            """
            CREATE TABLE IF NOT EXISTS event_categories (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT NOT NULL UNIQUE COLLATE NOCASE,
                description TEXT NOT NULL DEFAULT ''
            )""",
            """
            CREATE TABLE IF NOT EXISTS donors (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                name              TEXT NOT NULL,
                email             TEXT,
                phone             TEXT,
                social_handle     TEXT,
                preferred_channel TEXT NOT NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS donations (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                donor_id         INTEGER NOT NULL REFERENCES donors(id),
                donation_type_id INTEGER NOT NULL REFERENCES donation_types(id),
                method           TEXT    NOT NULL,
                amount           REAL,
                transaction_ref  TEXT,
                item_description TEXT,
                quantity         INTEGER,
                estimated_value  REAL,
                created_at       TEXT    NOT NULL,
                urgent           INTEGER NOT NULL DEFAULT 0,
                gift_honoree     TEXT,
                anonymous        INTEGER NOT NULL DEFAULT 0
            )""",
            """
            CREATE TABLE IF NOT EXISTS receipts (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                donation_id INTEGER NOT NULL UNIQUE REFERENCES donations(id) ON DELETE CASCADE,
                content     TEXT    NOT NULL,
                created_at  TEXT    NOT NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS volunteers (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                name         TEXT NOT NULL,
                email        TEXT,
                phone        TEXT,
                skills       TEXT,
                availability TEXT
            )""",
            """
            CREATE TABLE IF NOT EXISTS events (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT    NOT NULL,
                category_id INTEGER NOT NULL REFERENCES event_categories(id),
                event_date  TEXT    NOT NULL,
                location    TEXT,
                description TEXT
            )""",
            """
            CREATE TABLE IF NOT EXISTS event_volunteers (
                event_id     INTEGER NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                volunteer_id INTEGER NOT NULL REFERENCES volunteers(id) ON DELETE CASCADE,
                PRIMARY KEY (event_id, volunteer_id)
            )""",
            """
            CREATE TABLE IF NOT EXISTS attendees (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id INTEGER NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                name     TEXT    NOT NULL,
                email    TEXT,
                phone    TEXT
            )""",
            """
            CREATE TABLE IF NOT EXISTS beneficiaries (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                name    TEXT NOT NULL,
                email   TEXT,
                phone   TEXT,
                address TEXT,
                needs   TEXT
            )""",
            """
            CREATE TABLE IF NOT EXISTS message_log (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                channel   TEXT NOT NULL,
                recipient TEXT NOT NULL,
                subject   TEXT NOT NULL,
                body      TEXT NOT NULL,
                status    TEXT NOT NULL,
                sent_at   TEXT NOT NULL
            )"""
    };
}
