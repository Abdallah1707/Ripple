package com.ripple.extras;

import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract <b>Decorator</b> in the Decorator pattern for donations: it <i>is a</i>
 * {@link Donation} and <i>has a</i> {@link Donation} (the one it wraps), and forwards every
 * method to it. A concrete decorator ({@link UrgentDonation}, {@link GiftDonation},
 * {@link AnonymousDonation}) overrides only the one or two methods it changes, so decorators
 * can be stacked in any combination without a subclass for each combination.
 *
 * <p>{@code Donation} is an abstract class, not an interface, so this class also inherits its
 * fields. They are filled with the wrapped donation's values but never read: every getter is
 * forwarded instead. It is the same shape as {@code CondimentDecorator extends Beverage} in
 * <i>Head First Design Patterns</i>.
 */
public abstract class DonationDecorator extends Donation {

    protected final Donation wrapped;

    /**
     * Wraps a donation.
     *
     * @param wrapped the donation to decorate (a plain donation or another decorator)
     */
    protected DonationDecorator(Donation wrapped) {
        super(wrapped.getDonor(), wrapped.getDonationType(), wrapped.getCreatedAt());
        this.wrapped = wrapped;
    }

    // The id lives in the plain donation, so the receipt number is the same at every layer.
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

    /** The wrapped donation's options plus this decorator's own, for {@link #getOptions()}. */
    protected List<String> optionsPlus(String option) {
        List<String> options = new ArrayList<>(wrapped.getOptions());
        options.add(option);
        return List.copyOf(options);
    }
}
