package com.ripple.service;

import com.ripple.TestDatabase;
import com.ripple.model.Event;
import com.ripple.model.EventCategory;
import com.ripple.observer.EventObserver;
import com.ripple.repository.Database;
import com.ripple.repository.EventCategoryRepository;
import com.ripple.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the Observer subject for events: who is notified, when, and with what change text. */
class EventServiceTest {

    @TempDir
    Path tempDir;

    private EventService service;
    private EventCategory category;
    private final List<String> heard = new ArrayList<>();
    /** A stand-in observer that only records what it was told. */
    private final EventObserver recorder = (event, change) -> heard.add(event.getName() + ": " + change);

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        service = new EventService(new EventRepository(db));
        category = new EventCategoryRepository(db).findAll().get(0);
        service.registerObserver(recorder);
    }

    private Event event(int id, String name, String date, String location) {
        return new Event(id, name, category, LocalDate.parse(date), location, "");
    }

    @Test
    void addingAnEventNotifiesNobody() {
        Event saved = service.addEvent(event(0, "Food drive", "2026-12-15", "Cairo"));
        assertTrue(saved.getId() > 0);
        assertTrue(heard.isEmpty());
    }

    @Test
    void aNewDateOrLocationNotifiesWithTheChangeInWords() {
        int id = service.addEvent(event(0, "Food drive", "2026-12-15", "Cairo")).getId();

        assertTrue(service.updateEvent(event(id, "Food drive", "2026-12-20", "Giza")));

        assertEquals(List.of("Food drive: date changed from 2026-12-15 to 2026-12-20; "
                + "location changed from 'Cairo' to 'Giza'"), heard);
    }

    @Test
    void aNewNameOnlyNotifiesNobodyButIsStillSaved() {
        int id = service.addEvent(event(0, "Food drive", "2026-12-15", "Cairo")).getId();

        assertFalse(service.updateEvent(event(id, "Winter food drive", "2026-12-15", "Cairo")));

        assertTrue(heard.isEmpty());
    }

    @Test
    void aRemovedObserverIsNotNotifiedAndRegisteringTwiceHasNoEffect() {
        service.registerObserver(recorder); // second time: still one entry
        int id = service.addEvent(event(0, "Food drive", "2026-12-15", "Cairo")).getId();
        service.updateEvent(event(id, "Food drive", "2026-12-16", "Cairo"));
        assertEquals(1, heard.size());

        service.removeObserver(recorder);
        service.updateEvent(event(id, "Food drive", "2026-12-17", "Cairo"));
        assertEquals(1, heard.size());
        assertFalse(service.isRegistered(recorder));
    }

    @Test
    void aFailingObserverDoesNotStopTheOthers() {
        service.registerObserver((event, change) -> {
            throw new IllegalStateException("boom");
        });
        service.registerObserver(recorder); // already registered: stays first
        EventObserver second = (event, change) -> heard.add("second");
        service.registerObserver(second);
        int id = service.addEvent(event(0, "Food drive", "2026-12-15", "Cairo")).getId();

        assertTrue(service.updateEvent(event(id, "Food drive", "2026-12-16", "Cairo")));
        assertTrue(heard.contains("second"));
    }

    @Test
    void updatingAMissingEventIsRefused() {
        assertThrows(ValidationException.class, () -> service.updateEvent(event(999, "Ghost", "2026-12-15", "")));
    }
}
