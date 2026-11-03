package com.ripple.model;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * A donation of goods. Its value is the estimated value.
 */
public class InKindDonation extends Donation {

    private final String itemDescription;
    private final int quantity;
    private final double estimatedValue;

    /** Creates an in-kind donation. Use InKindDonationFactory instead. */
    public InKindDonation(Donor donor, DonationType donationType, LocalDateTime createdAt,
                          String itemDescription, int quantity, double estimatedValue) {
        super(donor, donationType, createdAt);
        this.itemDescription = itemDescription;
        this.quantity = quantity;
        this.estimatedValue = estimatedValue;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getEstimatedValue() {
        return estimatedValue;
    }

    @Override
    public DonationMethod getMethod() {
        return DonationMethod.IN_KIND;
    }

    @Override
    public double getValue() {
        return estimatedValue;
    }

    @Override
    public String describe() {
        return String.format(Locale.US, "%d x %s (est. EGP %,.2f)", quantity, itemDescription, estimatedValue);
    }
}
