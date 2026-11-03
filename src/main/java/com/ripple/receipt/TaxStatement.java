package com.ripple.receipt;

import com.ripple.model.Donation;
import com.ripple.model.Donor;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * A donor's yearly tax statement: their donations to tax-deductible types in one year
 * and the total deductible amount.
 */
public class TaxStatement {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Donor donor;
    private final int year;
    private final List<Donation> donations;

    /** Creates a statement. The donations are already filtered by donor, year and tax type. */
    public TaxStatement(Donor donor, int year, List<Donation> donations) {
        this.donor = donor;
        this.year = year;
        this.donations = List.copyOf(donations);
    }

    public Donor getDonor() {
        return donor;
    }

    public int getYear() {
        return year;
    }

    public List<Donation> getDonations() {
        return donations;
    }

    /** Total of the listed donations in EGP. */
    public double getTotalDeductible() {
        return donations.stream().mapToDouble(Donation::getValue).sum();
    }

    /** The statement as text. */
    public String render() {
        StringBuilder text = new StringBuilder(String.format(Locale.US, "Tax statement %d%nDonor: %s%n",
                year, donor.getName())); // real name, the statement is for the donor
        if (donations.isEmpty()) {
            text.append(String.format("No tax-deductible donations in %d.%n", year));
        } else {
            text.append(String.format("Tax-deductible donations:%n"));
            for (Donation donation : donations) {
                text.append(String.format(Locale.US, "%s  %s  %s  %s  EGP %,.2f%n",
                        donation.getReceiptNumber(), donation.getCreatedAt().format(DATE_FORMAT),
                        donation.getDonationType().getName(), donation.getMethod().getLabel(), donation.getValue()));
            }
        }
        text.append(String.format(Locale.US, "Total deductible amount: EGP %,.2f", getTotalDeductible()));
        return text.toString();
    }
}
