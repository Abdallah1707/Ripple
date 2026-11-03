package com.ripple.extras;

import com.ripple.model.CashDonation;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the Decorator pattern for donations: urgent, gift and anonymous, alone and stacked. */
class DonationDecoratorTest {

    private final Donor mona = new Donor(1, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
    private final DonationType zakat = new DonationType(1, "Zakat", "", true);

    private CashDonation cash() {
        return new CashDonation(mona, zakat, LocalDateTime.of(2026, 11, 3, 10, 0), 500);
    }

    @Test
    void aPlainDonationHasNoExtras() {
        Donation plain = cash();
        assertFalse(plain.isUrgent());
        assertFalse(plain.isAnonymous());
        assertEquals("", plain.getHonoree());
        assertEquals("Mona Ali", plain.getDonorDisplayName());
        assertEquals(List.of(), plain.getOptions());
        assertSame(plain, plain.getBase());
    }

    @Test
    void eachDecoratorChangesOnlyItsOwnPart() {
        Donation urgent = new UrgentDonation(cash());
        assertTrue(urgent.isUrgent());
        assertEquals("URGENT: Cash donation of EGP 500.00", urgent.describe());
        assertEquals("Mona Ali", urgent.getDonorDisplayName());

        Donation gift = new GiftDonation(cash(), "Hana Ali");
        assertEquals("Hana Ali", gift.getHonoree());
        assertEquals("Cash donation of EGP 500.00 (gift in honor of Hana Ali)", gift.describe());
        assertFalse(gift.isUrgent());

        Donation anonymous = new AnonymousDonation(cash());
        assertTrue(anonymous.isAnonymous());
        assertEquals(AnonymousDonation.DISPLAY_NAME, anonymous.getDonorDisplayName());
        assertEquals("Cash donation of EGP 500.00", anonymous.describe()); // the description is unchanged
    }

    @Test
    void decoratorsStackAndKeepEverythingElseOfTheDonation() {
        CashDonation base = cash();
        Donation all = new AnonymousDonation(new UrgentDonation(new GiftDonation(base, "Hana Ali")));

        assertEquals("URGENT: Cash donation of EGP 500.00 (gift in honor of Hana Ali)", all.describe());
        assertEquals(List.of("Gift", "Urgent", "Anonymous"), all.getOptions());
        assertTrue(all.isUrgent() && all.isAnonymous());
        assertEquals("Hana Ali", all.getHonoree());
        // everything else is forwarded to the plain donation
        assertEquals(500, all.getValue());
        assertEquals(DonationMethod.CASH, all.getMethod());
        assertTrue(all.isTaxDeductible());
        assertSame(base, all.getBase());
        assertSame(mona, all.getDonor()); // the real donor is kept: they still get their receipt
    }

    @Test
    void theIdIsSharedSoEveryLayerHasTheSameReceiptNumber() {
        CashDonation base = cash();
        Donation decorated = new UrgentDonation(new AnonymousDonation(base));

        decorated.setId(7); // what the repository does after saving

        assertEquals(7, base.getId());
        assertEquals("RCP-2026-0007", decorated.getReceiptNumber());
        assertEquals(base.getReceiptNumber(), decorated.getReceiptNumber());
    }

    @Test
    void optionsWrapInAFixedOrderAndNoneMeansNoWrapping() {
        CashDonation base = cash();
        assertSame(base, DonationOptions.NONE.decorate(base));

        Donation decorated = new DonationOptions(true, " Hana Ali ", true).decorate(base);
        assertEquals(List.of("Gift", "Urgent", "Anonymous"), decorated.getOptions());
        assertEquals("Hana Ali", decorated.getHonoree()); // trimmed

        assertFalse(new DonationOptions(false, null, false).isGift());
    }
}
