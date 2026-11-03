package com.ripple.repository;

import com.ripple.extras.DonationOptions;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.model.CashDonation;
import com.ripple.model.ChequeDonation;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.model.InKindDonation;
import com.ripple.model.OnlineDonation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Database access for donations. Loaded rows are rebuilt with the right factory
 * and their extras are put back as decorators.
 */
public class DonationRepository {

    private final Database database;
    private final Map<DonationMethod, DonationFactory> factories;

    /** Creates the repository. */
    public DonationRepository(Database database, Map<DonationMethod, DonationFactory> factories) {
        this.database = database;
        this.factories = factories;
    }

    /** Saves a donation and sets its id. Decorated donations save their extras too. */
    public Donation insert(Donation donation) {
        String sql = "INSERT INTO donations (donor_id, donation_type_id, method, amount, transaction_ref, "
                + "item_description, quantity, estimated_value, created_at, urgent, gift_honoree, anonymous) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Donation base = donation.getBase(); // plain donation without decorators

        // only fill the columns for this donation type
        Double amount = null;
        String transactionRef = null;
        String item = null;
        Integer quantity = null;
        Double estimatedValue = null;
        if (base instanceof CashDonation cash) {
            amount = cash.getAmount();
        } else if (base instanceof OnlineDonation online) {
            amount = online.getAmount();
            transactionRef = online.getTransactionReference();
        } else if (base instanceof ChequeDonation cheque) {
            amount = cheque.getAmount();
            transactionRef = cheque.getChequeNumber(); // cheque number uses the same column
        } else if (base instanceof InKindDonation inKind) {
            item = inKind.getItemDescription();
            quantity = inKind.getQuantity();
            estimatedValue = inKind.getEstimatedValue();
        }

        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, donation.getDonor().getId());
            statement.setInt(2, donation.getDonationType().getId());
            statement.setString(3, donation.getMethod().name());
            statement.setObject(4, amount);
            statement.setObject(5, transactionRef);
            statement.setObject(6, item);
            statement.setObject(7, quantity);
            statement.setObject(8, estimatedValue);
            statement.setString(9, donation.getCreatedAt().toString());
            statement.setInt(10, donation.isUrgent() ? 1 : 0);
            statement.setString(11, donation.getHonoree().isEmpty() ? null : donation.getHonoree());
            statement.setInt(12, donation.isAnonymous() ? 1 : 0);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                donation.setId(keys.getInt(1));
            }
            return donation;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** All donations, urgent first, then newest first. */
    public List<Donation> findAll() {
        String sql = """
                SELECT d.id, d.method, d.amount, d.transaction_ref, d.item_description,
                       d.quantity, d.estimated_value, d.created_at, d.urgent, d.gift_honoree, d.anonymous,
                       o.id AS donor_id, o.name AS donor_name, o.email AS donor_email,
                       o.phone AS donor_phone, o.social_handle, o.preferred_channel,
                       t.id AS type_id, t.name AS type_name, t.description AS type_description,
                       t.tax_deductible
                FROM donations d
                JOIN donors o ON o.id = d.donor_id
                JOIN donation_types t ON t.id = d.donation_type_id
                ORDER BY d.urgent DESC, d.id DESC
                """;
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<Donation> result = new ArrayList<>();
            while (rows.next()) {
                result.add(toDonation(rows));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Number of donations. */
    public int count() {
        try (Connection c = database.getConnection();
             Statement statement = c.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM donations")) {
            return rows.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Total value of all donations in EGP. */
    public double totalValue() {
        String sql = "SELECT COALESCE(SUM(CASE WHEN method = 'IN_KIND' THEN estimated_value ELSE amount END), 0) "
                + "FROM donations";
        try (Connection c = database.getConnection();
             Statement statement = c.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            return rows.getDouble(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Builds a donation from a row. */
    private Donation toDonation(ResultSet row) throws SQLException {
        Donor donor = new Donor(row.getInt("donor_id"), row.getString("donor_name"),
                row.getString("donor_email"), row.getString("donor_phone"), row.getString("social_handle"),
                ContactChannel.valueOf(row.getString("preferred_channel")));
        DonationType type = new DonationType(row.getInt("type_id"), row.getString("type_name"),
                row.getString("type_description"), row.getInt("tax_deductible") == 1);
        DonationRequest request = new DonationRequest(
                row.getDouble("amount"),
                row.getString("transaction_ref"),
                row.getString("item_description"),
                row.getInt("quantity"),
                row.getDouble("estimated_value"),
                LocalDateTime.parse(row.getString("created_at"))); // keep the saved time (CR3)
        DonationMethod method = DonationMethod.valueOf(row.getString("method")); // saved as the enum name
        DonationFactory factory = factories.get(method);
        if (factory == null) {
            throw new IllegalStateException("No creator for stored donation method " + method);
        }
        Donation base = factory.orderDonation(donor, type, request);
        base.setId(row.getInt("id"));
        DonationOptions options = new DonationOptions(row.getInt("urgent") == 1, row.getString("gift_honoree"),
                row.getInt("anonymous") == 1);
        return options.decorate(base); // add the extras back
    }
}
