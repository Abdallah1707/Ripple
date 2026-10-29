package com.ripple.repository;

import com.ripple.model.EventCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reads and writes the {@code event_categories} lookup table. */
public class EventCategoryRepository implements LookupRepository<EventCategory> {

    private final Database database;

    /**
     * Creates the repository.
     *
     * @param database the database to use
     */
    public EventCategoryRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<EventCategory> findAll() {
        String sql = "SELECT id, name, description FROM event_categories ORDER BY name COLLATE NOCASE";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<EventCategory> result = new ArrayList<>();
            while (rows.next()) {
                result.add(toEventCategory(rows));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<EventCategory> findById(int id) {
        String sql = "SELECT id, name, description FROM event_categories WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(toEventCategory(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByName(String name, int excludeId) {
        String sql = "SELECT COUNT(*) FROM event_categories WHERE name = ? COLLATE NOCASE AND id <> ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setInt(2, excludeId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public EventCategory insert(EventCategory category) {
        String sql = "INSERT INTO event_categories (name, description) VALUES (?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                category.setId(keys.getInt(1));
            }
            return category;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(EventCategory category) {
        String sql = "UPDATE event_categories SET name = ?, description = ? WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setInt(3, category.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement("DELETE FROM event_categories WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public int countUsages(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement("SELECT COUNT(*) FROM events WHERE category_id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    private static EventCategory toEventCategory(ResultSet row) throws SQLException {
        return new EventCategory(row.getInt("id"), row.getString("name"), row.getString("description"));
    }
}
