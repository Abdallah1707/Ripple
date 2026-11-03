package com.ripple.receipt;

import com.ripple.TestFactories;
import com.ripple.factory.DonationRequest;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for TaxStatement. */
class TaxStatementTest {

    private final Donor mona = new Donor(1, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
    private final DonationType zakat = new DonationType(1, "Zakat", "", true);

    private Donation cash(int id, double amount, LocalDateTime when) {
        Donation donation = TestFactories.create(DonationMethod.CASH, mona, zakat,
                new DonationRequest(amount, null, null, 0, 0, when));
        donation.setId(id);
        return donation;
    }

    @Test
    void listsEachDonationAndTheTotal() {
        TaxStatement statement = new TaxStatement(mona, 2026, List.of(
                cash(3, 500, LocalDateTime.of(2026, 2, 1, 10, 0)),
                cash(7, 1250.5, LocalDateTime.of(2026, 6, 15, 12, 30))));

        assertEquals(1750.5, statement.getTotalDeductible(), 0.001);
        String text = statement.render();
        assertTrue(text.startsWith("Tax statement 2026"), text);
        assertTrue(text.contains("Donor: Mona Ali"), text);
        assertTrue(text.contains("RCP-2026-0003  2026-02-01  Zakat  Cash  EGP 500.00"), text);
        assertTrue(text.contains("RCP-2026-0007  2026-06-15  Zakat  Cash  EGP 1,250.50"), text);
        assertTrue(text.endsWith("Total deductible amount: EGP 1,750.50"), text);
    }

    @Test
    void aYearWithoutTaxDeductibleDonationsSaysSo() {
        TaxStatement statement = new TaxStatement(mona, 2025, List.of());

        assertEquals(0, statement.getTotalDeductible());
        assertTrue(statement.render().contains("No tax-deductible donations in 2025."));
        assertTrue(statement.render().endsWith("Total deductible amount: EGP 0.00"));
    }
}
