package com.ripple.repository;

import com.ripple.model.Attendee;
import com.ripple.model.Event;
import com.ripple.model.EventCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes the {@code events} and {@code attendees} tables. Contains SQL only,
 * no business rules. Deleting an event also deletes its attendees and volunteer
 * assignments (ON DELETE CASCADE in the schema).
 */
public class EventRepository {

    /** Events are always read together with their category, so the entity is complete. */
    private static final String SELECT_EVENT =
            "SELECT e.id, e.name, e.event_date, e.location, e.description, "
                    + "c.id AS category_id, c.name AS category_name, c.description AS category_description "
                    + "FROM events e JOIN event_categories c ON c.id = e.category_id ";

    private final Database database;

    /**
     * Creates the repository.
     *
     * @param database the database to use
     */
    public EventRepository(Database database) {
        this.database = database;
    }

    /**
     * Returns all events, soonest first.
     *
     * @return all events
     */
    public List<Event> findAll() {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(
                     SELECT_EVENT + "ORDER BY e.event_date, e.name COLLATE NOCASE");
             ResultSet rows = statement.executeQuery()) {
            List<Event> result = new ArrayList<>();
            while (rows.next()) {
                result.add(toEvent(rows));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Finds one event.
     *
     * @param id the event's id
     * @return the event, or empty if there is no event with that id
     */
    public Optional<Event> findById(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(SELECT_EVENT + "WHERE e.id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(toEvent(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Saves a new event and sets its generated id.
     *
     * @param event the event to save (its id is ignored)
     * @return the same event, now with its id
     */
    public Event insert(Event event) {
        String sql = "INSERT INTO events (name, category_id, event_date, location, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, event.getName());
            statement.setInt(2, event.getCategory().getId());
            statement.setString(3, event.getEventDate().toString());
            statement.setString(4, event.getLocation());
            statement.setString(5, event.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                event.setId(keys.getInt(1));
            }
            return event;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing event (matched by id).
     *
     * @param event the event with its new values
     */
    public void update(Event event) {
        String sql = "UPDATE events SET name = ?, category_id = ?, event_date = ?, location = ?, description = ? "
                + "WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, event.getName());
            statement.setInt(2, event.getCategory().getId());
            statement.setString(3, event.getEventDate().toString());
            statement.setString(4, event.getLocation());
            statement.setString(5, event.getDescription());
            statement.setInt(6, event.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an event together with its attendees and volunteer assignments.
     *
     * @param id the event's id
     */
    public void delete(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement("DELETE FROM events WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Counts the events (used by the Dashboard).
     *
     * @return the number of events
     */
    public int count() {
        try (Connection c = database.getConnection();
             Statement statement = c.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM events")) {
            return rows.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Registers an attendee for an event and sets the attendee's generated id.
     *
     * @param attendee the attendee to save (its id is ignored)
     * @return the same attendee, now with its id
     */
    public Attendee addAttendee(Attendee attendee) {
        String sql = "INSERT INTO attendees (event_id, name, email, phone) VALUES (?, ?, ?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, attendee.getEventId());
            statement.setString(2, attendee.getName());
            statement.setString(3, attendee.getEmail());
            statement.setString(4, attendee.getPhone());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                attendee.setId(keys.getInt(1));
            }
            return attendee;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the attendees registered for an event, sorted by name.
     *
     * @param eventId the event's id
     * @return the attendees (empty if none)
     */
    public List<Attendee> findAttendees(int eventId) {
        String sql = "SELECT id, event_id, name, email, phone FROM attendees WHERE event_id = ? "
                + "ORDER BY name COLLATE NOCASE";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Attendee> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(new Attendee(rows.getInt("id"), rows.getInt("event_id"), rows.getString("name"),
                            rows.getString("email"), rows.getString("phone")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    private static Event toEvent(ResultSet row) throws SQLException {
        EventCategory category = new EventCategory(row.getInt("category_id"), row.getString("category_name"),
                row.getString("category_description"));
        return new Event(row.getInt("id"), row.getString("name"), category,
                LocalDate.parse(row.getString("event_date")), row.getString("location"),
                row.getString("description"));
    }
}
