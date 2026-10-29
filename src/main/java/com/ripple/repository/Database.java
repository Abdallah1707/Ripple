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
 * Gives the repositories connections to Ripple's SQLite database file, and creates the
 * schema and starting data the first time the app runs.
 *
 * <p>The schema here contains <b>every</b> table of Phase 1 (see the ER diagram in
 * {@code docs/architecture.md}), so later sections never have to change this class.
 *
 * <p>Not a Singleton on purpose: each test creates its own {@code Database} on a
 * temporary file, so tests never share data.
 */
public class Database {

    /** Default location of the database file, relative to the folder Ripple is started from. */
    public static final String DEFAULT_PATH = "data/ripple.db";

    private final String url;

    /**
     * Creates a database object for the given file. The file itself is created by SQLite
     * on the first connection; the parent folder is created here if it is missing.
     *
     * @param path path of the SQLite file, e.g. {@code data/ripple.db}
     * @throws IllegalStateException if the parent folder cannot be created
     */
    public Database(String path) {
        Path file = Path.of(path).toAbsolutePath();
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create folder for database: " + file.getParent(), e);
        }
        this.url = "jdbc:sqlite:" + file;
    }

    /**
     * Creates the database the app uses: the path in the {@code RIPPLE_DB_PATH} environment
     * variable, or {@link #DEFAULT_PATH} if it is not set.
     *
     * @return the application database
     */
    public static Database fromEnvironment() {
        String path = System.getenv("RIPPLE_DB_PATH");
        return new Database(path == null || path.isBlank() ? DEFAULT_PATH : path);
    }

    /**
     * Opens a new connection with foreign keys switched on. SQLite turns them off by default,
     * and they must be enabled on every connection. Close it with try-with-resources.
     *
     * @return an open connection
     * @throws SQLException if the database cannot be opened
     */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /**
     * Creates all tables that do not exist yet and adds the starting lookup data if the
     * lookup tables are empty. Safe to call on every start.
     *
     * @throws IllegalStateException if the database cannot be created
     */
    public void initialize() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : SCHEMA) {
                statement.execute(sql);
            }
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

    /** Inserts {@code rows} into {@code table} only if the table has no rows yet. */
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
     * All Phase 1 tables. Lookup names are unique ignoring case (CR1). Records that use a
     * lookup value have no ON DELETE CASCADE, so a lookup in use cannot be deleted (CR2).
     * Dates and times are stored as ISO-8601 text.
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
                created_at       TEXT    NOT NULL
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
