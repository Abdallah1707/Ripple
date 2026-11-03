package com.ripple;

import com.ripple.repository.Database;

import java.nio.file.Path;

/**
 * Test helper that makes a fresh database in a temp folder for each test.
 */
public final class TestDatabase {

    private TestDatabase() {
    }

    /** Creates and sets up a database in tempDir. */
    public static Database create(Path tempDir) {
        Database database = new Database(tempDir.resolve("test.db").toString());
        database.initialize();
        return database;
    }
}
