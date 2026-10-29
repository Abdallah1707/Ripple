package com.ripple.repository;

import com.ripple.model.DonationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reads and writes the {@code donation_types} lookup table. */
public class DonationTypeRepository implements LookupRepository<DonationType> {

    private final Database database;

    /**
     * Creates the repository.
     *
     * @param database the database to use
     */
    public DonationTypeRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<DonationType> findAll() {
        String sql = "SELECT id, name, description, tax_deductible FROM donation_types ORDER BY name COLLATE NOCASE";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<DonationType> result = new ArrayList<>();
            while (rows.next()) {
                result.add(toDonationType(rows));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<DonationType> findById(int id) {
        String sql = "SELECT id, name, description, tax_deductible FROM donation_types WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(toDonationType(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByName(String name, int excludeId) {
        String sql = "SELECT COUNT(*) FROM donation_types WHERE name = ? COLLATE NOCASE AND id <> ?";
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
    public DonationType insert(DonationType type) {
        String sql = "INSERT INTO donation_types (name, description, tax_deductible) VALUES (?, ?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, type.getName());
            statement.setString(2, type.getDescription());
            statement.setInt(3, type.isTaxDeductible() ? 1 : 0);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                type.setId(keys.getInt(1));
            }
            return type;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(DonationType type) {
        String sql = "UPDATE donation_types SET name = ?, description = ?, tax_deductible = ? WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, type.getName());
            statement.setString(2, type.getDescription());
            statement.setInt(3, type.isTaxDeductible() ? 1 : 0);
            statement.setInt(4, type.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement("DELETE FROM donation_types WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public int countUsages(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(
                     "SELECT COUNT(*) FROM donations WHERE donation_type_id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    private static DonationType toDonationType(ResultSet row) throws SQLException {
        return new DonationType(row.getInt("id"), row.getString("name"),
                row.getString("description"), row.getInt("tax_deductible") == 1);
    }
}
