package com.ripple.observer;

import com.ripple.TestDatabase;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.logging.ApplicationLogger;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.Donor;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.service.DonationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that DonationLogObserver writes a line to the application log. */
class DonationLogObserverTest {

    @TempDir
    Path tempDir;

    @Test
    void writesOneLineWithTheReceiptNumber() {
        Donor donor = new Donor(1, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
        Donation donation = new DonationFactory().create(DonationMethod.CASH, donor,
                new com.ripple.model.DonationType(1, "Zakat", "", true),
                new DonationRequest(500, null, null, 0, 0, null));
        donation.setId(7);

        new DonationLogObserver().onDonationRecorded(donation);

        String latest = ApplicationLogger.getInstance().getRecentEntries(1).get(0);
        assertTrue(latest.contains(donation.getReceiptNumber()));
        assertTrue(latest.contains("Mona Ali"));
    }

    @Test
    void recordingADonationLogsItWhenTheObserverIsAttached() {
        Database db = TestDatabase.create(tempDir);
        DonationFactory factory = new DonationFactory();
        DonationTypeRepository types = new DonationTypeRepository(db);
        DonationService service = new DonationService(factory, new DonorRepository(db),
                new DonationRepository(db, factory), types);
        service.attach(new DonationLogObserver());
        Donor donor = service.registerDonor("Omar", "omar@example.com", "", ContactChannel.EMAIL);

        Donation donation = service.recordDonation(DonationMethod.CASH, donor.getId(),
                types.findAll().get(0).getId(), new DonationRequest(75, null, null, 0, 0, null));

        String latest = ApplicationLogger.getInstance().getRecentEntries(1).get(0);
        assertTrue(latest.contains(donation.getReceiptNumber()));
        assertEquals(1, ApplicationLogger.getInstance().getRecentEntries(5).stream()
                .filter(entry -> entry.contains(donation.getReceiptNumber())).count());
    }
}
