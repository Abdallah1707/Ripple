package com.ripple.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationTest {

    private final Donor donor = new Donor(1, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
    private final DonationType zakat = new DonationType(1, "Zakat", "", true);
    private final DonationType food = new DonationType(2, "Food Supplies", "", false);
    private final LocalDateTime time = LocalDateTime.of(2026, 3, 14, 10, 30);

    @Test
    void receiptNumberUsesYearAndFourDigitId() {
        Donation donation = new CashDonation(donor, zakat, time, 500);
        donation.setId(7);
        assertEquals("RCP-2026-0007", donation.getReceiptNumber());
    }

    @Test
    void taxDeductibleFollowsTheDonationType() {
        assertTrue(new CashDonation(donor, zakat, time, 500).isTaxDeductible());
        assertFalse(new CashDonation(donor, food, time, 500).isTaxDeductible());
    }

    @Test
    void eachKindHasItsOwnValueAndDescription() {
        Donation cash = new CashDonation(donor, zakat, time, 1250);
        Donation online = new OnlineDonation(donor, zakat, time, 750, "TX-1001");
        Donation goods = new InKindDonation(donor, food, time, "Blankets", 20, 4000);

        assertEquals(1250, cash.getValue());
        assertEquals("Cash donation of EGP 1,250.00", cash.describe());
        assertEquals("Online donation of EGP 750.00 (ref TX-1001)", online.describe());
        assertEquals(4000, goods.getValue());
        assertEquals("20 x Blankets (est. EGP 4,000.00)", goods.describe());
        assertEquals(DonationMethod.IN_KIND, goods.getMethod());
    }
}
