package com.ripple.logging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationLoggerTest {

    @Test
    void getInstanceAlwaysReturnsTheSameObject() {
        assertSame(ApplicationLogger.getInstance(), ApplicationLogger.getInstance());
    }

    @Test
    void recentEntriesAreNewestFirst() {
        ApplicationLogger logger = ApplicationLogger.getInstance();
        logger.info("first test entry");
        logger.error("second test entry");

        List<String> recent = logger.getRecentEntries(2);
        assertTrue(recent.get(0).endsWith("ERROR second test entry"), recent.get(0));
        assertTrue(recent.get(1).endsWith("INFO first test entry"), recent.get(1));
    }

    @Test
    void getRecentEntriesRespectsTheLimit() {
        ApplicationLogger logger = ApplicationLogger.getInstance();
        for (int i = 0; i < 5; i++) {
            logger.info("entry " + i);
        }
        assertEquals(3, logger.getRecentEntries(3).size());
    }
}
