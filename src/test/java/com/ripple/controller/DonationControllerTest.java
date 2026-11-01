package com.ripple.controller;

import com.ripple.TestDatabase;
import com.ripple.factory.DonationFactory;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.Donor;
import com.ripple.observer.DonorNotificationObserver;
import com.ripple.observer.ReceiptObserver;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.ReceiptRepository;
import com.ripple.service.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

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
    private DonationTypeRepository types;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        DonationFactory factory = new DonationFactory();
        types = new DonationTypeRepository(db);
        DonationService service = new DonationService(factory, new DonorRepository(db),
                new DonationRepository(db, factory), types);
        DonorNotificationObserver notifier = new DonorNotificationObserver();
        service.attach(notifier); // like Main: notifications start on
        ReceiptRepository receiptRepository = new ReceiptRepository(db);
        service.attach(new ReceiptObserver(receiptRepository));
        controller = new DonationController(service, receiptRepository, notifier);
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
    void recordingADonationSavesAReceiptWithTaxInfoOnlyWhenTheTypeIsTaxDeductible() {
        int zakatId = types.findAll().stream()
                .filter(t -> t.getName().equals("Zakat")).findFirst().orElseThrow().getId();
        int foodSuppliesId = types.findAll().stream()
                .filter(t -> t.getName().equals("Food Supplies")).findFirst().orElseThrow().getId();

        controller.recordDonation(donor.getId(), zakatId, "CASH", "500", "", "", "", "");
        controller.recordDonation(donor.getId(), foodSuppliesId, "CASH", "200", "", "", "", "");

        List<Donation> donations = controller.listDonations(); // newest first
        String foodSuppliesReceipt = controller.getReceipt(donations.get(0).getId());
        String zakatReceipt = controller.getReceipt(donations.get(1).getId());

        assertTrue(zakatReceipt.contains("Thank you"));
        assertTrue(zakatReceipt.contains("tax-deductible"));
        assertTrue(foodSuppliesReceipt.contains("Thank you"));
        assertFalse(foodSuppliesReceipt.contains("tax-deductible"));
    }

    @Test
    void registerDonorChecksChannelRules() {
        assertTrue(controller.registerDonor("Omar", "omar@example.com", "", "EMAIL").success());
        assertFalse(controller.registerDonor("Sara", "", "", "SMS").success());
        assertFalse(controller.registerDonor("Sara", "sara@example.com", "", "FAX").success());
        assertEquals(2, controller.listDonors().size()); // Mona (setUp) + Omar
    }
}
