package com.ripple.factory;

import com.ripple.model.CashDonation;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.model.InKindDonation;
import com.ripple.model.OnlineDonation;
import com.ripple.service.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DonationFactoryTest {

    private final DonationFactory factory = new DonationFactory();
    private final Donor donor = new Donor(1, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
    private final DonationType zakat = new DonationType(1, "Zakat", "", true);

    private static DonationRequest money(double amount, String reference) {
        return new DonationRequest(amount, reference, null, 0, 0, null);
    }

    private static DonationRequest goods(String item, int quantity, double value) {
        return new DonationRequest(0, null, item, quantity, value, null);
    }

    @Test
    void createsTheRightSubclassForEachMethod() {
        Donation cash = factory.create(DonationMethod.CASH, donor, zakat, money(500, null));
        Donation online = factory.create(DonationMethod.ONLINE, donor, zakat, money(750, "TX-1001"));
        Donation goods = factory.create(DonationMethod.IN_KIND, donor, zakat, goods("Blankets", 20, 4000));

        assertInstanceOf(CashDonation.class, cash);
        assertInstanceOf(OnlineDonation.class, online);
        assertInstanceOf(InKindDonation.class, goods);
        assertEquals(500, cash.getValue());
        assertEquals(750, online.getValue());
        assertEquals(4000, goods.getValue());
    }

    @Test
    void newDonationsAreTimeStamped() {
        assertNotNull(factory.create(DonationMethod.CASH, donor, zakat, money(10, null)).getCreatedAt());
    }

    @Test
    void storedTimestampIsKept() {
        LocalDateTime stored = LocalDateTime.of(2025, 12, 1, 9, 0);
        DonationRequest request = new DonationRequest(10, null, null, 0, 0, stored);
        assertEquals(stored, factory.create(DonationMethod.CASH, donor, zakat, request).getCreatedAt());
    }

    @Test
    void rejectsZeroOrNegativeAmounts() {
        assertEquals("Amount must be greater than 0", assertThrows(ValidationException.class,
                () -> factory.create(DonationMethod.CASH, donor, zakat, money(0, null))).getMessage());
        assertThrows(ValidationException.class,
                () -> factory.create(DonationMethod.ONLINE, donor, zakat, money(-5, "TX-1")));
    }

    @Test
    void onlineDonationNeedsTransactionReference() {
        assertEquals("Transaction reference is required", assertThrows(ValidationException.class,
                () -> factory.create(DonationMethod.ONLINE, donor, zakat, money(100, " "))).getMessage());
    }

    @Test
    void inKindDonationNeedsItemAndPositiveQuantity() {
        assertEquals("Quantity must be greater than 0", assertThrows(ValidationException.class,
                () -> factory.create(DonationMethod.IN_KIND, donor, zakat, goods("Blankets", 0, 100))).getMessage());
        assertThrows(ValidationException.class,
                () -> factory.create(DonationMethod.IN_KIND, donor, zakat, goods("", 5, 100)));
        assertThrows(ValidationException.class,
                () -> factory.create(DonationMethod.IN_KIND, donor, zakat, goods("Blankets", 5, -1)));
    }

    @Test
    void missingDonorOrTypeIsRejected() {
        assertThrows(ValidationException.class, () -> factory.create(DonationMethod.CASH, null, zakat, money(10, null)));
        assertThrows(ValidationException.class, () -> factory.create(DonationMethod.CASH, donor, null, money(10, null)));
        assertThrows(ValidationException.class, () -> factory.create(null, donor, zakat, money(10, null)));
    }

    @Test
    void parseMethodAcceptsNamesAndLabels() {
        assertEquals(DonationMethod.ONLINE, factory.parseMethod(" online "));
        assertEquals(DonationMethod.IN_KIND, factory.parseMethod("In-kind"));
        assertEquals(DonationMethod.IN_KIND, factory.parseMethod("IN_KIND"));
    }

    @Test
    void parseMethodRejectsUnsupportedMethods() {
        assertEquals("Unsupported donation method: cheque",
                assertThrows(ValidationException.class, () -> factory.parseMethod("cheque")).getMessage());
        assertThrows(ValidationException.class, () -> factory.parseMethod(""));
    }
}
