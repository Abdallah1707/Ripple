package com.ripple.repository;

import com.ripple.model.Volunteer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the {@code volunteers} table and its link table {@code event_volunteers}.
 * Contains SQL only, no business rules.
 */
public class VolunteerRepository {

    private static final String COLUMNS = "id, name, email, phone, skills, availability";

    private final Database database;

    /**
     * Creates the repository.
     *
     * @param database the database to use
     */
    public VolunteerRepository(Database database) {
        this.database = database;
    }

    /**
     * Returns all volunteers sorted by name.
     *
     * @return all volunteers
     */
    public List<Volunteer> findAll() {
        return query("SELECT " + COLUMNS + " FROM volunteers ORDER BY name COLLATE NOCASE", null);
    }

    /**
     * Saves a new volunteer and sets its generated id.
     *
     * @param volunteer the volunteer to save (its id is ignored)
     * @return the same volunteer, now with its id
     */
    public Volunteer insert(Volunteer volunteer) {
        String sql = "INSERT INTO volunteers (name, email, phone, skills, availability) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, volunteer.getName());
            statement.setString(2, volunteer.getEmail());
            statement.setString(3, volunteer.getPhone());
            statement.setString(4, volunteer.getSkills());
            statement.setString(5, volunteer.getAvailability());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                volunteer.setId(keys.getInt(1));
            }
            return volunteer;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing volunteer (matched by id).
     *
     * @param volunteer the volunteer with its new values
     */
    public void update(Volunteer volunteer) {
        String sql = "UPDATE volunteers SET name = ?, email = ?, phone = ?, skills = ?, availability = ? WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, volunteer.getName());
            statement.setString(2, volunteer.getEmail());
            statement.setString(3, volunteer.getPhone());
            statement.setString(4, volunteer.getSkills());
            statement.setString(5, volunteer.getAvailability());
            statement.setInt(6, volunteer.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a volunteer. Their event assignments are removed too (ON DELETE CASCADE).
     *
     * @param id the volunteer's id
     */
    public void delete(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement("DELETE FROM volunteers WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Counts the volunteers (used by the Dashboard).
     *
     * @return the number of volunteers
     */
    public int count() {
        try (Connection c = database.getConnection();
             Statement statement = c.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM volunteers")) {
            return rows.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Assigns a volunteer to an event. Assigning the same pair twice does nothing, so no
     * duplicate row is created ({@code INSERT OR IGNORE} on the primary key).
     *
     * @param volunteerId the volunteer's id
     * @param eventId     the event's id
     */
    public void assignToEvent(int volunteerId, int eventId) {
        String sql = "INSERT OR IGNORE INTO event_volunteers (event_id, volunteer_id) VALUES (?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setInt(2, volunteerId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the volunteers assigned to an event, sorted by name.
     *
     * @param eventId the event's id
     * @return the assigned volunteers (empty if none)
     */
    public List<Volunteer> findByEvent(int eventId) {
        String sql = "SELECT v.id, v.name, v.email, v.phone, v.skills, v.availability FROM volunteers v "
                + "JOIN event_volunteers ev ON ev.volunteer_id = v.id WHERE ev.event_id = ? "
                + "ORDER BY v.name COLLATE NOCASE";
        return query(sql, eventId);
    }

    private List<Volunteer> query(String sql, Integer parameter) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setInt(1, parameter);
            }
            try (ResultSet rows = statement.executeQuery()) {
                List<Volunteer> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(new Volunteer(rows.getInt("id"), rows.getString("name"), rows.getString("email"),
                            rows.getString("phone"), rows.getString("skills"), rows.getString("availability")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }
}
