package com.ripple.observer;

import com.ripple.TestDatabase;
import com.ripple.TestFactories;
import com.ripple.communication.CommunicationService;
import com.ripple.communication.EmailStrategy;
import com.ripple.communication.MessageLogEntry;
import com.ripple.communication.SmsStrategy;
import com.ripple.communication.SocialMediaStrategy;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.Donor;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.MessageLogRepository;
import com.ripple.service.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that donors get a thank-you on their preferred channel (CR5). */
class DonorNotificationObserverTest {

    @TempDir
    Path tempDir;

    private DonationService service;
    private MessageLogRepository log;
    private DonorNotificationObserver notifier;
    private int typeId;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        Map<DonationMethod, DonationFactory> factories = TestFactories.all();
        DonationTypeRepository types = new DonationTypeRepository(db);
        service = new DonationService(factories, new DonorRepository(db), new DonationRepository(db, factories), types);
        log = new MessageLogRepository(db);
        notifier = new DonorNotificationObserver(new CommunicationService(log),
                Map.of(ContactChannel.EMAIL, new EmailStrategy(), ContactChannel.SMS, new SmsStrategy(),
                        ContactChannel.SOCIAL, new SocialMediaStrategy()));
        service.registerObserver(notifier);
        typeId = types.findAll().get(0).getId();
    }

    private Donation donate(Donor donor) {
        return service.recordDonation(DonationMethod.CASH, donor.getId(), typeId,
                new DonationRequest(100, null, null, 0, 0, null));
    }

    @Test
    void thankYouUsesEachDonorsPreferredChannel() {
        Donor byEmail = service.registerDonor("Mona Ali", "mona@example.com", "01012345678", "", ContactChannel.EMAIL);
        Donor bySms = service.registerDonor("Omar Samy", "omar@example.com", "01198765432", "", ContactChannel.SMS);

        Donation first = donate(byEmail);
        donate(bySms);

        List<MessageLogEntry> entries = log.findAll(); // newest first
        assertEquals(2, entries.size());
        assertEquals(ContactChannel.SMS, entries.get(0).channel());
        assertEquals("Omar Samy <01198765432>", entries.get(0).recipient());
        assertEquals(ContactChannel.EMAIL, entries.get(1).channel());
        assertEquals("Mona Ali <mona@example.com>", entries.get(1).recipient());
        assertTrue(entries.get(1).body().contains(first.getReceiptNumber()));
        assertEquals("SIMULATED", entries.get(1).status());
    }

    @Test
    void socialDonorIsThankedOnSocialMediaWithNoChangeToTheObserver() {
        Donor donor = service.registerDonor("Sara", "", "", "@sara_s", ContactChannel.SOCIAL);

        donate(donor);

        MessageLogEntry entry = log.findAll().get(0);
        assertEquals(ContactChannel.SOCIAL, entry.channel());
        assertEquals("Sara <@sara_s>", entry.recipient());
        assertEquals("SIMULATED", entry.status());
    }

    @Test
    void noThankYouIsSentAfterTheObserverIsDetached() {
        Donor donor = service.registerDonor("Mona Ali", "mona@example.com", "", "", ContactChannel.EMAIL);
        donate(donor);
        assertEquals(1, log.findAll().size());

        service.removeObserver(notifier);
        donate(donor);
        assertEquals(1, log.findAll().size());

        service.registerObserver(notifier);
        donate(donor);
        assertEquals(2, log.findAll().size());
    }
}
