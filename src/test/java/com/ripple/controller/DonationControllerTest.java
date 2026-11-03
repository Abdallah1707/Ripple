package com.ripple.controller;

import com.ripple.TestDatabase;
import com.ripple.TestFactories;
import com.ripple.communication.CommunicationService;
import com.ripple.communication.EmailStrategy;
import com.ripple.communication.SmsStrategy;
import com.ripple.factory.DonationFactory;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.Donor;
import com.ripple.observer.DonorNotificationObserver;
import com.ripple.observer.ReceiptObserver;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.MessageLogRepository;
import com.ripple.repository.ReceiptRepository;
import com.ripple.service.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
        Map<DonationMethod, DonationFactory> factories = TestFactories.all();
        types = new DonationTypeRepository(db);
        DonationService service = new DonationService(factories, new DonorRepository(db),
                new DonationRepository(db, factories), types);
        DonorNotificationObserver notifier = new DonorNotificationObserver(
                new CommunicationService(new MessageLogRepository(db)),
                Map.of(ContactChannel.EMAIL, new EmailStrategy(), ContactChannel.SMS, new SmsStrategy()));
        service.registerObserver(notifier); // like Main: notifications start on
        ReceiptRepository receiptRepository = new ReceiptRepository(db);
        service.registerObserver(new ReceiptObserver(receiptRepository));
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
        assertEquals("Cheque number is required",
                controller.recordDonation(donor.getId(), typeId, "CHEQUE", "100", "", "", "", "").message());
        assertEquals("Unsupported donation method: Bitcoin",
                controller.recordDonation(donor.getId(), typeId, "Bitcoin", "100", "", "", "", "").message());
        assertFalse(controller.recordDonation(donor.getId(), typeId, "", "100", "", "", "", "").success());
        assertTrue(controller.listDonations().isEmpty());
    }

    @Test
    void chequeDonationIsRecordedWithItsNumber() {
        Result result = controller.recordDonation(donor.getId(), typeId, "CHEQUE", "1500", "000123", "", "", "");
        assertTrue(result.success(), result.message());
        assertEquals("Cheque donation of EGP 1,500.00 (cheque no. 000123)",
                controller.listDonations().get(0).describe());
    }

    @Test
    void methodNamesAndLabelsAreBothAccepted() {
        assertTrue(controller.recordDonation(donor.getId(), typeId, " online ", "10", "TX-1", "", "", "").success());
        assertTrue(controller.recordDonation(donor.getId(), typeId, "In-kind", "", "", "Books", "2", "50").success());
        assertTrue(controller.recordDonation(donor.getId(), typeId, "Cheque", "10", "CHQ-9", "", "", "").success());
        assertEquals(3, controller.listDonations().size());
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
    void saveDonorWithoutIdRegistersAndChecksChannelRules() {
        assertTrue(controller.saveDonor(null, "Omar", "omar@example.com", "", "EMAIL").success());
        assertFalse(controller.saveDonor(null, "Sara", "", "", "SMS").success());
        assertFalse(controller.saveDonor(null, "Sara", "sara@example.com", "", "FAX").success());
        assertEquals(2, controller.listDonors().size()); // Mona (setUp) + Omar
    }

    @Test
    void saveDonorWithIdUpdatesTheDonor() {
        Result result = controller.saveDonor(donor.getId(), "Mona A. Ali", "", "01012345678", "SMS");
        assertTrue(result.success(), result.message());
        assertEquals("Donor 'Mona A. Ali' updated", result.message());
        Donor updated = controller.listDonors().get(0);
        assertEquals("Mona A. Ali", updated.getName());
        assertEquals(ContactChannel.SMS, updated.getPreferredChannel());
    }

    @Test
    void deleteDonorIsBlockedWhileTheDonorHasDonations() {
        controller.recordDonation(donor.getId(), typeId, "CASH", "500", "", "", "", "");

        Result blocked = controller.deleteDonor(donor.getId());
        assertFalse(blocked.success());
        assertEquals("'Mona Ali' has 1 donation(s) and cannot be deleted", blocked.message());
        assertEquals(1, controller.listDonors().size());
    }

    @Test
    void deleteDonorRemovesADonorWithoutDonations() {
        Result result = controller.deleteDonor(donor.getId());
        assertTrue(result.success(), result.message());
        assertTrue(controller.listDonors().isEmpty());
        assertFalse(controller.deleteDonor(donor.getId()).success()); // already gone
    }
}
