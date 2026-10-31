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

/** Reads and writes the {@code donors} table. Contains SQL only, no business rules. */
public class DonorRepository {

    private final Database database;

    /**
     * Creates the repository.
     *
     * @param database the database to use
     */
    public DonorRepository(Database database) {
        this.database = database;
    }

    /**
     * Returns all donors sorted by name.
     *
     * @return all donors
     */
    public List<Donor> findAll() {
        String sql = "SELECT id, name, email, phone, preferred_channel FROM donors ORDER BY name COLLATE NOCASE";
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

    /**
     * Finds one donor.
     *
     * @param id the donor's id
     * @return the donor, or empty if there is no donor with that id
     */
    public Optional<Donor> findById(int id) {
        String sql = "SELECT id, name, email, phone, preferred_channel FROM donors WHERE id = ?";
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

    /**
     * Saves a new donor and sets its generated id.
     *
     * @param donor the donor to save (its id is ignored)
     * @return the same donor, now with its id
     */
    public Donor insert(Donor donor) {
        String sql = "INSERT INTO donors (name, email, phone, preferred_channel) VALUES (?, ?, ?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, donor.getName());
            statement.setString(2, donor.getEmail());
            statement.setString(3, donor.getPhone());
            statement.setString(4, donor.getPreferredChannel().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                donor.setId(keys.getInt(1));
            }
            return donor;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Counts the donors (used by the Dashboard).
     *
     * @return the number of donors
     */
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
                row.getString("phone"), ContactChannel.valueOf(row.getString("preferred_channel")));
    }
}
