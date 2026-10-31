package com.ripple.receipt;

import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that BasicReceipt has no extra sections and each decorator adds only its own. */
class ReceiptDecoratorTest {

    private final Donor donor = new Donor(1, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);

    private Donation newDonation() {
        DonationType type = new DonationType(1, "Zakat", "", true);
        Donation donation = new DonationFactory().create(DonationMethod.CASH, donor, type,
                new DonationRequest(500, null, null, 0, 0, null));
        donation.setId(7);
        return donation;
    }

    @Test
    void basicReceiptHasNoThankYouAndNoTaxSection() {
        String text = new BasicReceipt(newDonation()).render();

        assertTrue(text.contains("Receipt RCP-"));
        assertTrue(text.contains("Mona Ali"));
        assertFalse(text.contains("Thank you"));
        assertFalse(text.contains("tax-deductible"));
    }

    @Test
    void thankYouMessageDecoratorAddsItsSectionOnTopOfTheBasicReceipt() {
        Donation donation = newDonation();
        String basic = new BasicReceipt(donation).render();

        String decorated = new ThankYouMessageDecorator(new BasicReceipt(donation), donor.getName()).render();

        assertTrue(decorated.startsWith(basic));
        assertTrue(decorated.contains("Thank you, Mona Ali, for your generous support."));
        assertFalse(decorated.contains("tax-deductible"));
    }

    @Test
    void taxInformationDecoratorAddsItsSectionOnTopOfWhateverItWraps() {
        Donation donation = newDonation();
        Receipt inner = new ThankYouMessageDecorator(new BasicReceipt(donation), donor.getName());
        String withoutTax = inner.render();

        String withTax = new TaxInformationDecorator(inner, donation).render();

        assertTrue(withTax.startsWith(withoutTax));
        assertTrue(withTax.contains("tax-deductible"));
        assertTrue(withTax.contains("500"));
    }
}
