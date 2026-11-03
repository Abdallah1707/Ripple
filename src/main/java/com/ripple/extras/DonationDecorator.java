package com.ripple.extras;

import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base decorator for donations. It wraps a donation and forwards every method to it.
 * Subclasses only override what they change, so they can be stacked in any combination.
 */
public abstract class DonationDecorator extends Donation {

    protected final Donation wrapped;

    /** Wraps a donation. */
    protected DonationDecorator(Donation wrapped) {
        super(wrapped.getDonor(), wrapped.getDonationType(), wrapped.getCreatedAt());
        this.wrapped = wrapped;
    }

    // same id at every layer
    @Override
    public int getId() {
        return wrapped.getId();
    }

    @Override
    public void setId(int id) {
        wrapped.setId(id);
    }

    @Override
    public Donor getDonor() {
        return wrapped.getDonor();
    }

    @Override
    public DonationType getDonationType() {
        return wrapped.getDonationType();
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return wrapped.getCreatedAt();
    }

    @Override
    public DonationMethod getMethod() {
        return wrapped.getMethod();
    }

    @Override
    public double getValue() {
        return wrapped.getValue();
    }

    @Override
    public String describe() {
        return wrapped.describe();
    }

    @Override
    public String getReceiptNumber() {
        return wrapped.getReceiptNumber();
    }

    @Override
    public boolean isTaxDeductible() {
        return wrapped.isTaxDeductible();
    }

    @Override
    public Donation getBase() {
        return wrapped.getBase();
    }

    @Override
    public String getDonorDisplayName() {
        return wrapped.getDonorDisplayName();
    }

    @Override
    public boolean isUrgent() {
        return wrapped.isUrgent();
    }

    @Override
    public String getHonoree() {
        return wrapped.getHonoree();
    }

    @Override
    public boolean isAnonymous() {
        return wrapped.isAnonymous();
    }

    @Override
    public List<String> getOptions() {
        return wrapped.getOptions();
    }

    /** The wrapped donation's options plus this one. */
    protected List<String> optionsPlus(String option) {
        List<String> options = new ArrayList<>(wrapped.getOptions());
        options.add(option);
        return List.copyOf(options);
    }
}
