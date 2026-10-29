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
 * Singleton in the Singleton pattern: the one shared application log.
 *
 * <p>Every entry is appended to {@code logs/ripple.log} and kept in a short in-memory
 * list that the Dashboard shows as "recent activity".
 *
 * <p><b>Why this is a Singleton:</b> there is one log file and one activity feed. Two
 * logger objects would write to the same file independently and each would only see
 * part of the activity. It is the only Singleton in Ripple; everything else is passed
 * through constructors.
 *
 * <p><b>How the single instance is guaranteed:</b> the constructor is private, so no
 * other class can call {@code new ApplicationLogger()}. The instance is created once when
 * the JVM loads this class, which Java guarantees is thread-safe without {@code synchronized}.
 */
public final class ApplicationLogger {

    private static final ApplicationLogger INSTANCE = new ApplicationLogger();

    private static final int MAX_RECENT_ENTRIES = 100;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFile = Path.of("logs", "ripple.log");
    /** Newest entry first. */
    private final Deque<String> recentEntries = new ArrayDeque<>();

    private ApplicationLogger() {
        // private: use getInstance()
    }

    /**
     * Returns the one and only logger.
     *
     * @return the shared logger
     */
    public static ApplicationLogger getInstance() {
        return INSTANCE;
    }

    /**
     * Records a normal event, e.g. "Donation type 'Zakat' added".
     *
     * @param message what happened
     */
    public synchronized void info(String message) {
        write("INFO", message);
    }

    /**
     * Records a problem, e.g. a database error.
     *
     * @param message what went wrong
     */
    public synchronized void error(String message) {
        write("ERROR", message);
    }

    /**
     * Returns the most recent entries, newest first.
     *
     * @param limit maximum number of entries to return
     * @return a copy of up to {@code limit} entries
     */
    public synchronized List<String> getRecentEntries(int limit) {
        return recentEntries.stream().limit(limit).toList();
    }

    /** Adds the entry to the in-memory list and appends it to the log file. */
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
            // Logging must never crash the app; the entry is still in recentEntries.
            System.err.println("Could not write to " + logFile + ": " + e.getMessage());
        }
    }
}
