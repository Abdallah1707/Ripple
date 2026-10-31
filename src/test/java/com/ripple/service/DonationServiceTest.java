package com.ripple.service;

import com.ripple.TestDatabase;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.model.CashDonation;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.Donor;
import com.ripple.model.InKindDonation;
import com.ripple.model.OnlineDonation;
import com.ripple.observer.DonationObserver;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the donor rules, recording donations, and the Observer behaviour of DonationService. */
class DonationServiceTest {

    /** A simple observer that counts how many times it was told about a donation. */
    private static class CountingObserver implements DonationObserver {
        int calls = 0;
        Donation last;

        @Override
        public void onDonationRecorded(Donation donation) {
            calls++;
            last = donation;
        }
    }

    /** An observer that always fails. */
    private static class BrokenObserver implements DonationObserver {
        @Override
        public void onDonationRecorded(Donation donation) {
            throw new IllegalStateException("broken on purpose");
        }
    }

    @TempDir
    Path tempDir;

    private DonationService service;
    private Donor donor;
    private int typeId;
    private CountingObserver observer;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        DonationFactory factory = new DonationFactory();
        DonationTypeRepository types = new DonationTypeRepository(db);
        service = new DonationService(factory, new DonorRepository(db),
                new DonationRepository(db, factory), types);
        donor = service.registerDonor("Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
        typeId = types.findAll().get(0).getId();
        observer = new CountingObserver();
        service.attach(observer);
    }

    private DonationRequest cash(double amount) {
        return new DonationRequest(amount, null, null, 0, 0, null);
    }

    // ---------------- donor rules ----------------

    @Test
    void registerDonorSavesDonorWithPreferredChannel() {
        Donor saved = service.registerDonor("  Omar  ", "", "010 1234 5678", ContactChannel.SMS);
        assertTrue(saved.getId() > 0);
        assertEquals("Omar", saved.getName());
        assertEquals(ContactChannel.SMS, service.findAllDonors().stream()
                .filter(d -> d.getId() == saved.getId()).findFirst().orElseThrow().getPreferredChannel());
    }

    @Test
    void donorNameIsRequired() {
        assertThrows(ValidationException.class,
                () -> service.registerDonor("   ", "a@b.com", "", ContactChannel.EMAIL));
    }

    @Test
    void emailDonorNeedsValidEmail() {
        assertThrows(ValidationException.class,
                () -> service.registerDonor("Sara", "not-an-email", "", ContactChannel.EMAIL));
        assertThrows(ValidationException.class,
                () -> service.registerDonor("Sara", "", "", ContactChannel.EMAIL));
    }

    @Test
    void smsDonorNeedsValidPhone() {
        assertThrows(ValidationException.class,
                () -> service.registerDonor("Sara", "sara@example.com", "abc", ContactChannel.SMS));
        assertThrows(ValidationException.class,
                () -> service.registerDonor("Sara", "sara@example.com", "", ContactChannel.SMS));
    }

    @Test
    void phoneNumberMustBeExactly11Digits() {
        assertThrows(ValidationException.class,
                () -> service.registerDonor("Sara", "sara@example.com", "0101234567", ContactChannel.EMAIL));   // 10 digits
        assertThrows(ValidationException.class,
                () -> service.registerDonor("Sara", "sara@example.com", "010123456789", ContactChannel.EMAIL)); // 12 digits
        assertThrows(ValidationException.class,
                () -> service.registerDonor("Sara", "", "0101234567", ContactChannel.SMS));
        // exactly 11 digits is accepted (spaces and dashes are ignored)
        assertTrue(service.registerDonor("Sara", "sara@example.com", "01012345678", ContactChannel.EMAIL).getId() > 0);
        assertTrue(service.registerDonor("Omar", "", "010-1234-5678", ContactChannel.SMS).getId() > 0);
    }

    // ---------------- recording each kind of donation ----------------

    @Test
    void recordsCashDonation() {
        Donation donation = service.recordDonation(DonationMethod.CASH, donor.getId(), typeId, cash(500));
        assertInstanceOf(CashDonation.class, donation);
        assertTrue(donation.getId() > 0);
        assertTrue(donation.getReceiptNumber().startsWith("RCP-"));
        assertEquals(1, service.findAllDonations().size());
    }

    @Test
    void recordsOnlineDonation() {
        DonationRequest request = new DonationRequest(250, "TX-1001", null, 0, 0, null);
        Donation donation = service.recordDonation(DonationMethod.ONLINE, donor.getId(), typeId, request);
        assertInstanceOf(OnlineDonation.class, donation);
        assertEquals(250, donation.getValue());
    }

    @Test
    void recordsInKindDonation() {
        DonationRequest request = new DonationRequest(0, null, "Blankets", 10, 1500, null);
        Donation donation = service.recordDonation(DonationMethod.IN_KIND, donor.getId(), typeId, request);
        assertInstanceOf(InKindDonation.class, donation);
        assertEquals(1500, donation.getValue());
    }

    // ---------------- bad input ----------------

    @Test
    void badInputIsRejectedAndNoObserverIsTold() {
        DonationRequest noQuantity = new DonationRequest(0, null, "Blankets", 0, 100, null);
        DonationRequest noReference = new DonationRequest(100, "", null, 0, 0, null);

        assertThrows(ValidationException.class,
                () -> service.recordDonation(DonationMethod.IN_KIND, donor.getId(), typeId, noQuantity));
        assertThrows(ValidationException.class,
                () -> service.recordDonation(DonationMethod.ONLINE, donor.getId(), typeId, noReference));
        assertThrows(ValidationException.class,
                () -> service.recordDonation(DonationMethod.CASH, donor.getId(), typeId, cash(0)));

        assertEquals(0, observer.calls);
        assertTrue(service.findAllDonations().isEmpty()); // nothing was saved either
    }

    @Test
    void unknownDonorOrTypeIsRejected() {
        assertThrows(ValidationException.class,
                () -> service.recordDonation(DonationMethod.CASH, 9999, typeId, cash(10)));
        assertThrows(ValidationException.class,
                () -> service.recordDonation(DonationMethod.CASH, donor.getId(), 9999, cash(10)));
        assertEquals(0, observer.calls);
    }

    // ---------------- Observer behaviour ----------------

    @Test
    void attachedObserverIsNotifiedWithTheSavedDonation() {
        Donation donation = service.recordDonation(DonationMethod.CASH, donor.getId(), typeId, cash(100));
        assertEquals(1, observer.calls);
        assertEquals(donation.getId(), observer.last.getId()); // it already has its id
    }

    @Test
    void detachedObserverIsNotNotified() {
        service.detach(observer);
        service.recordDonation(DonationMethod.CASH, donor.getId(), typeId, cash(100));
        assertEquals(0, observer.calls);
        assertFalse(service.isAttached(observer));
    }

    @Test
    void attachingTwiceNotifiesOnlyOnce() {
        service.attach(observer); // already attached in setUp
        service.recordDonation(DonationMethod.CASH, donor.getId(), typeId, cash(100));
        assertEquals(1, observer.calls);
    }

    @Test
    void aFailingObserverDoesNotStopTheOthers() {
        CountingObserver after = new CountingObserver();
        service.attach(new BrokenObserver());
        service.attach(after);

        service.recordDonation(DonationMethod.CASH, donor.getId(), typeId, cash(100));

        assertEquals(1, observer.calls);
        assertEquals(1, after.calls);
        assertEquals(1, service.findAllDonations().size()); // the donation is still saved
    }
}
