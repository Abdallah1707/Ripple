package com.ripple.repository;

import com.ripple.model.Beneficiary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes the {@code beneficiaries} table. Contains SQL only, no business rules. */
public class BeneficiaryRepository {

    private final Database database;

    /**
     * Creates the repository.
     *
     * @param database the database to use
     */
    public BeneficiaryRepository(Database database) {
        this.database = database;
    }

    /**
     * Returns all beneficiaries sorted by name.
     *
     * @return all beneficiaries
     */
    public List<Beneficiary> findAll() {
        String sql = "SELECT id, name, email, phone, address, needs FROM beneficiaries ORDER BY name COLLATE NOCASE";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<Beneficiary> result = new ArrayList<>();
            while (rows.next()) {
                result.add(new Beneficiary(rows.getInt("id"), rows.getString("name"), rows.getString("email"),
                        rows.getString("phone"), rows.getString("address"), rows.getString("needs")));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Saves a new beneficiary and sets its generated id.
     *
     * @param beneficiary the beneficiary to save (its id is ignored)
     * @return the same beneficiary, now with its id
     */
    public Beneficiary insert(Beneficiary beneficiary) {
        String sql = "INSERT INTO beneficiaries (name, email, phone, address, needs) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, beneficiary.getName());
            statement.setString(2, beneficiary.getEmail());
            statement.setString(3, beneficiary.getPhone());
            statement.setString(4, beneficiary.getAddress());
            statement.setString(5, beneficiary.getNeeds());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                beneficiary.setId(keys.getInt(1));
            }
            return beneficiary;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing beneficiary (matched by id).
     *
     * @param beneficiary the beneficiary with its new values
     */
    public void update(Beneficiary beneficiary) {
        String sql = "UPDATE beneficiaries SET name = ?, email = ?, phone = ?, address = ?, needs = ? WHERE id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, beneficiary.getName());
            statement.setString(2, beneficiary.getEmail());
            statement.setString(3, beneficiary.getPhone());
            statement.setString(4, beneficiary.getAddress());
            statement.setString(5, beneficiary.getNeeds());
            statement.setInt(6, beneficiary.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a beneficiary.
     *
     * @param id the beneficiary's id
     */
    public void delete(int id) {
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement("DELETE FROM beneficiaries WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Counts the beneficiaries (used by the Dashboard).
     *
     * @return the number of beneficiaries
     */
    public int count() {
        try (Connection c = database.getConnection();
             Statement statement = c.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM beneficiaries")) {
            return rows.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }
}
