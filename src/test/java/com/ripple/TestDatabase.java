package com.ripple;

import com.ripple.repository.Database;

import java.nio.file.Path;

/**
 * Test helper: gives each test its own fresh, initialized database in a temporary folder,
 * so tests never touch {@code data/ripple.db} and never affect each other.
 *
 * <pre>{@code
 * @TempDir Path tempDir;
 * Database db = TestDatabase.create(tempDir);
 * }</pre>
 */
public final class TestDatabase {

    private TestDatabase() {
    }

    /**
     * Creates and initializes a database file inside {@code tempDir}.
     *
     * @param tempDir a JUnit {@code @TempDir} folder
     * @return a ready-to-use database with the schema and seed data
     */
    public static Database create(Path tempDir) {
        Database database = new Database(tempDir.resolve("test.db").toString());
        database.initialize();
        return database;
    }
}
