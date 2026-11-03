package com.ripple.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Singleton logger. Writes to logs/ripple.log and keeps recent entries for the dashboard.
 * The instance is created when the class loads.
 */
public final class ApplicationLogger {

    private static final ApplicationLogger uniqueInstance = new ApplicationLogger();

    private static final int MAX_RECENT_ENTRIES = 100;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFile = Path.of("logs", "ripple.log");
    /** Newest first. */
    private final Deque<String> recentEntries = new ArrayDeque<>();

    private ApplicationLogger() {
        // use getInstance()
    }

    /** Returns the logger. */
    public static ApplicationLogger getInstance() {
        return uniqueInstance;
    }

    /** Logs a normal event. */
    public synchronized void info(String message) {
        write("INFO", message);
    }

    /** Logs an error. */
    public synchronized void error(String message) {
        write("ERROR", message);
    }

    /** The latest entries, newest first. */
    public synchronized List<String> getRecentEntries(int limit) {
        return recentEntries.stream().limit(limit).toList();
    }

    /** Saves the entry in memory and in the log file. */
    private void write(String level, String message) {
        String entry = LocalDateTime.now().format(TIME_FORMAT) + " " + level + " " + message;
        recentEntries.addFirst(entry);
        if (recentEntries.size() > MAX_RECENT_ENTRIES) {
            recentEntries.removeLast();
        }
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, entry + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // logging shouldn't crash the app, the entry is still kept in memory
        }
    }
}
