package com.ripple.controller;

import com.ripple.TestDatabase;
import com.ripple.factory.DonationFactory;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donor;
import com.ripple.observer.DonorNotificationObserver;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.service.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that the controller turns screen texts into results and handles the notify checkbox. */
class DonationControllerTest {

    @TempDir
    Path tempDir;

    private DonationController controller;
    private Donor donor;
    private int typeId;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        DonationFactory factory = new DonationFactory();
        DonationTypeRepository types = new DonationTypeRepository(db);
        DonationService service = new DonationService(factory, new DonorRepository(db),
                new DonationRepository(db, factory), types);
        DonorNotificationObserver notifier = new DonorNotificationObserver();
        service.attach(notifier); // like Main: notifications start on
        controller = new DonationController(service, notifier);
        donor = service.registerDonor("Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
        typeId = types.findAll().get(0).getId();
    }

    @Test
    void notificationsStartOnAndCanBeToggled() {
        assertTrue(controller.isDonorNotificationsEnabled());
        controller.setDonorNotifications(false);
        assertFalse(controller.isDonorNotificationsEnabled());
        controller.setDonorNotifications(true);
        assertTrue(controller.isDonorNotificationsEnabled());
    }

    @Test
    void validDonationGivesGreenResultWithReceiptNumber() {
        Result result = controller.recordDonation(donor.getId(), typeId, "CASH", "500", "", "", "", "");
        assertTrue(result.success());
        assertTrue(result.message().contains("RCP-"));
        assertEquals(1, controller.listDonations().size());
    }

    @Test
    void onlyTheFieldsOfTheChosenMethodAreRead() {
        Result result = controller.recordDonation(donor.getId(), typeId, "IN_KIND", "abc", "", "Blankets", "3", "900");
        assertTrue(result.success()); // the bad "amount" text is ignored for in-kind
    }

    @Test
    void badInputGivesRedResultAndSavesNothing() {
        assertFalse(controller.recordDonation(donor.getId(), typeId, "CASH", "abc", "", "", "", "").success());
        assertFalse(controller.recordDonation(donor.getId(), typeId, "ONLINE", "100", "", "", "", "").success());
        assertFalse(controller.recordDonation(donor.getId(), typeId, "IN_KIND", "", "", "Blankets", "0", "100").success());
        assertFalse(controller.recordDonation(donor.getId(), typeId, "CHEQUE", "100", "", "", "", "").success());
        assertEquals("Unsupported donation method: CHEQUE",
                controller.recordDonation(donor.getId(), typeId, "CHEQUE", "100", "", "", "", "").message());
        assertTrue(controller.listDonations().isEmpty());
    }

    @Test
    void registerDonorChecksChannelRules() {
        assertTrue(controller.registerDonor("Omar", "omar@example.com", "", "EMAIL").success());
        assertFalse(controller.registerDonor("Sara", "", "", "SMS").success());
        assertFalse(controller.registerDonor("Sara", "sara@example.com", "", "FAX").success());
        assertEquals(2, controller.listDonors().size()); // Mona (setUp) + Omar
    }
}
