package com.ripple.repository;

import com.ripple.TestDatabase;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptRepositoryTest {

    @TempDir
    Path tempDir;

    private ReceiptRepository repository;
    private int donationId;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        repository = new ReceiptRepository(db);
        DonationFactory factory = new DonationFactory();
        Donor donor = new DonorRepository(db).insert(new Donor(0, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL));
        DonationType type = new DonationTypeRepository(db).findAll().get(0);
        Donation donation = new DonationRepository(db, factory).insert(
                factory.create(DonationMethod.CASH, donor, type, new DonationRequest(500, null, null, 0, 0, null)));
        donationId = donation.getId();
    }

    @Test
    void savedReceiptCanBeFoundByDonationId() {
        assertEquals(Optional.empty(), repository.findByDonationId(donationId));

        repository.save(donationId, "Receipt text");

        assertEquals(Optional.of("Receipt text"), repository.findByDonationId(donationId));
    }

    @Test
    void savingAgainForTheSameDonationReplacesThePreviousReceipt() {
        repository.save(donationId, "First version");
        repository.save(donationId, "Second version");

        Optional<String> found = repository.findByDonationId(donationId);
        assertTrue(found.isPresent());
        assertEquals("Second version", found.get());
    }
}
