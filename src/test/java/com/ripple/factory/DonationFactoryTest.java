package com.ripple.factory;

import com.ripple.model.CashDonation;
import com.ripple.model.ChequeDonation;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.model.InKindDonation;
import com.ripple.model.OnlineDonation;
import com.ripple.service.ValidationException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the Factory Method: each Concrete Creator and the shared {@code orderDonation} workflow. */
class DonationFactoryTest {

    private final DonationFactory cashFactory = new CashDonationFactory();
    private final DonationFactory onlineFactory = new OnlineDonationFactory();
    private final DonationFactory inKindFactory = new InKindDonationFactory();
    private final DonationFactory chequeFactory = new ChequeDonationFactory();
    private final Donor donor = new Donor(1, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
    private final DonationType zakat = new DonationType(1, "Zakat", "", true);

    private static DonationRequest money(double amount, String reference) {
        return new DonationRequest(amount, reference, null, 0, 0, null);
    }

    private static DonationRequest goods(String item, int quantity, double value) {
        return new DonationRequest(0, null, item, quantity, value, null);
    }

    @Test
    void eachCreatorMakesItsOwnSubclass() {
        Donation cash = cashFactory.orderDonation(donor, zakat, money(500, null));
        Donation online = onlineFactory.orderDonation(donor, zakat, money(750, "TX-1001"));
        Donation goods = inKindFactory.orderDonation(donor, zakat, goods("Blankets", 20, 4000));
        Donation cheque = chequeFactory.orderDonation(donor, zakat, money(1200, "000123"));

        assertInstanceOf(CashDonation.class, cash);
        assertInstanceOf(OnlineDonation.class, online);
        assertInstanceOf(InKindDonation.class, goods);
        assertInstanceOf(ChequeDonation.class, cheque);
        assertEquals(500, cash.getValue());
        assertEquals(750, online.getValue());
        assertEquals(4000, goods.getValue());
        assertEquals(1200, cheque.getValue());
    }

    @Test
    void eachCreatorReportsTheMethodItMakes() {
        assertEquals(DonationMethod.CASH, cashFactory.getMethod());
        assertEquals(DonationMethod.ONLINE, onlineFactory.getMethod());
        assertEquals(DonationMethod.IN_KIND, inKindFactory.getMethod());
        assertEquals(DonationMethod.CHEQUE, chequeFactory.getMethod());
    }

    @Test
    void theWorkflowCannotBeOverriddenBySubclasses() throws NoSuchMethodException {
        int modifiers = DonationFactory.class.getMethod("orderDonation",
                Donor.class, DonationType.class, DonationRequest.class).getModifiers();
        assertTrue(Modifier.isFinal(modifiers));
        assertTrue(Modifier.isAbstract(DonationFactory.class.getModifiers()));
    }

    @Test
    void newDonationsAreTimeStamped() {
        assertNotNull(cashFactory.orderDonation(donor, zakat, money(10, null)).getCreatedAt());
    }

    @Test
    void storedTimestampIsKept() {
        LocalDateTime stored = LocalDateTime.of(2025, 12, 1, 9, 0);
        DonationRequest request = new DonationRequest(10, null, null, 0, 0, stored);
        assertEquals(stored, cashFactory.orderDonation(donor, zakat, request).getCreatedAt());
    }

    @Test
    void rejectsZeroOrNegativeAmounts() {
        assertEquals("Amount must be greater than 0", assertThrows(ValidationException.class,
                () -> cashFactory.orderDonation(donor, zakat, money(0, null))).getMessage());
        assertThrows(ValidationException.class, () -> onlineFactory.orderDonation(donor, zakat, money(-5, "TX-1")));
        assertThrows(ValidationException.class, () -> chequeFactory.orderDonation(donor, zakat, money(0, "000123")));
    }

    @Test
    void onlineDonationNeedsTransactionReference() {
        assertEquals("Transaction reference is required", assertThrows(ValidationException.class,
                () -> onlineFactory.orderDonation(donor, zakat, money(100, " "))).getMessage());
    }

    @Test
    void chequeDonationNeedsChequeNumber() {
        assertEquals("Cheque number is required", assertThrows(ValidationException.class,
                () -> chequeFactory.orderDonation(donor, zakat, money(100, ""))).getMessage());
        assertThrows(ValidationException.class, () -> chequeFactory.orderDonation(donor, zakat, money(100, "12")));
    }

    @Test
    void inKindDonationNeedsItemAndPositiveQuantity() {
        assertEquals("Quantity must be greater than 0", assertThrows(ValidationException.class,
                () -> inKindFactory.orderDonation(donor, zakat, goods("Blankets", 0, 100))).getMessage());
        assertThrows(ValidationException.class, () -> inKindFactory.orderDonation(donor, zakat, goods("", 5, 100)));
        assertThrows(ValidationException.class, () -> inKindFactory.orderDonation(donor, zakat, goods("Blankets", 5, -1)));
    }

    @Test
    void theWorkflowRejectsMissingDonorTypeOrDetailsForEveryCreator() {
        DonationFactory[] creators = {cashFactory, onlineFactory, inKindFactory, chequeFactory};
        for (DonationFactory creator : creators) {
            assertThrows(ValidationException.class, () -> creator.orderDonation(null, zakat, money(10, "TX-1")));
            assertThrows(ValidationException.class, () -> creator.orderDonation(donor, null, money(10, "TX-1")));
            assertThrows(ValidationException.class, () -> creator.orderDonation(donor, zakat, null));
        }
    }
}
