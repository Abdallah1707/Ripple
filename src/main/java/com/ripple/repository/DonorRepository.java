package com.ripple.repository;

import com.ripple.model.ContactChannel;
import com.ripple.model.Donor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Database access for donors. */
public class DonorRepository {

    private final Database database;

    /** Creates the repository. */
    public DonorRepository(Database database) {
        this.database = database;
    }

    /** All donors sorted by name. */
    public List<Donor> findAll() {
        String sql = "SELECT id, name, email, phone, social_handle, preferred_channel FROM donors ORDER BY name COLLATE NOCASE";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<Donor> result = new ArrayList<>();
            while (rows.next()) {
                result.add(toDonor(rows));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Finds a donor by id. */
    public Optional<Donor> findById(int id) {
        String sql = "SELECT id, name, email, phone, social_handle, preferred_channel FROM donors WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(toDonor(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Saves a new donor and sets its id. */
    public Donor insert(Donor donor) {
        String sql = "INSERT INTO donors (name, email, phone, social_handle, preferred_channel) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, donor.getName());
            statement.setString(2, donor.getEmail());
            statement.setString(3, donor.getPhone());
            statement.setString(4, donor.getSocialHandle());
            statement.setString(5, donor.getPreferredChannel().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                donor.setId(keys.getInt(1));
            }
            return donor;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Updates a donor. */
    public void update(Donor donor) {
        String sql = "UPDATE donors SET name = ?, email = ?, phone = ?, social_handle = ?, preferred_channel = ? "
                + "WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, donor.getName());
            statement.setString(2, donor.getEmail());
            statement.setString(3, donor.getPhone());
            statement.setString(4, donor.getSocialHandle());
            statement.setString(5, donor.getPreferredChannel().name());
            statement.setInt(6, donor.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Deletes a donor. */
    public void delete(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement("DELETE FROM donors WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Number of donations by this donor. */
    public int countDonations(int donorId) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement("SELECT COUNT(*) FROM donations WHERE donor_id = ?")) {
            statement.setInt(1, donorId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Number of donors. */
    public int count() {
        try (Connection c = database.getConnection();
             Statement statement = c.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM donors")) {
            return rows.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    private static Donor toDonor(ResultSet row) throws SQLException {
        return new Donor(row.getInt("id"), row.getString("name"), row.getString("email"),
                row.getString("phone"), row.getString("social_handle"),
                ContactChannel.valueOf(row.getString("preferred_channel")));
    }
}
