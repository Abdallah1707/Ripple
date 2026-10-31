package com.ripple.observer;

import com.ripple.TestDatabase;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.ReceiptRepository;
import com.ripple.service.DonationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that ReceiptObserver builds the right decorator chain and saves it. */
class ReceiptObserverTest {

    @TempDir
    Path tempDir;

    @Test
    void buildReceiptAddsTaxInformationOnlyForATaxDeductibleType() {
        ReceiptObserver observer = new ReceiptObserver(new ReceiptRepository(TestDatabase.create(tempDir)));
        Donor donor = new Donor(1, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
        DonationFactory factory = new DonationFactory();

        Donation zakat = factory.create(DonationMethod.CASH, donor, new DonationType(1, "Zakat", "", true),
                new DonationRequest(500, null, null, 0, 0, null));
        zakat.setId(1);
        Donation foodSupplies = factory.create(DonationMethod.CASH, donor,
                new DonationType(2, "Food Supplies", "", false),
                new DonationRequest(200, null, null, 0, 0, null));
        foodSupplies.setId(2);

        String zakatReceipt = observer.buildReceipt(zakat).render();
        String foodSuppliesReceipt = observer.buildReceipt(foodSupplies).render();

        assertTrue(zakatReceipt.contains("Thank you"));
        assertTrue(zakatReceipt.contains("tax-deductible"));
        assertTrue(foodSuppliesReceipt.contains("Thank you"));
        assertFalse(foodSuppliesReceipt.contains("tax-deductible"));
    }

    @Test
    void recordingADonationSavesItsReceiptWhenTheObserverIsAttached() {
        Database db = TestDatabase.create(tempDir);
        DonationFactory factory = new DonationFactory();
        DonationTypeRepository types = new DonationTypeRepository(db);
        DonationService service = new DonationService(factory, new DonorRepository(db),
                new DonationRepository(db, factory), types);
        ReceiptRepository receiptRepository = new ReceiptRepository(db);
        service.attach(new ReceiptObserver(receiptRepository));
        Donor donor = service.registerDonor("Omar", "omar@example.com", "", ContactChannel.EMAIL);
        DonationType zakat = types.findAll().stream()
                .filter(t -> t.getName().equals("Zakat")).findFirst().orElseThrow();

        Donation donation = service.recordDonation(DonationMethod.CASH, donor.getId(), zakat.getId(),
                new DonationRequest(75, null, null, 0, 0, null));

        String saved = receiptRepository.findByDonationId(donation.getId()).orElseThrow();
        assertTrue(saved.contains(donation.getReceiptNumber()));
        assertTrue(saved.contains("tax-deductible"));
    }
}
