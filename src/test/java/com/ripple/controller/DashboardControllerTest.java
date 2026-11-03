package com.ripple.controller;

import com.ripple.TestDatabase;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.logging.ApplicationLogger;
import com.ripple.model.ContactChannel;
import com.ripple.model.DonationMethod;
import com.ripple.model.Donor;
import com.ripple.repository.BeneficiaryRepository;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.EventCategoryRepository;
import com.ripple.repository.EventRepository;
import com.ripple.repository.VolunteerRepository;
import com.ripple.service.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that the Dashboard's numbers follow the data and that its activity list comes from the logger. */
class DashboardControllerTest {

    @TempDir
    Path tempDir;

    private Database db;
    private DashboardController dashboard;
    private DonationService donationService;
    private DonationTypeRepository donationTypes;
    private VolunteerController volunteerController;
    private EventController eventController;
    private BeneficiaryController beneficiaryController;

    @BeforeEach
    void setUp() {
        db = TestDatabase.create(tempDir);
        DonationFactory factory = new DonationFactory();
        DonorRepository donors = new DonorRepository(db);
        DonationRepository donations = new DonationRepository(db, factory);
        VolunteerRepository volunteers = new VolunteerRepository(db);
        EventRepository events = new EventRepository(db);
        BeneficiaryRepository beneficiaries = new BeneficiaryRepository(db);
        donationTypes = new DonationTypeRepository(db);

        dashboard = new DashboardController(donors, donations, volunteers, events, beneficiaries);
        donationService = new DonationService(factory, donors, donations, donationTypes);
        volunteerController = new VolunteerController(volunteers);
        eventController = new EventController(events, volunteers);
        beneficiaryController = new BeneficiaryController(beneficiaries);
    }

    @Test
    void aFreshDatabaseShowsZeros() {
        DashboardSummary summary = dashboard.getSummary();

        assertEquals(0, summary.donors());
        assertEquals(0, summary.donations());
        assertEquals(0.0, summary.totalDonationValue(), 0.0001);
        assertEquals(0, summary.volunteers());
        assertEquals(0, summary.events());
        assertEquals(0, summary.beneficiaries());
    }

    @Test
    void numbersChangeAfterRecordingADonation() {
        Donor donor = donationService.registerDonor("Mona Ali", "mona@example.com", "", ContactChannel.EMAIL);
        int typeId = donationTypes.findAll().get(0).getId();
        assertEquals(1, dashboard.getSummary().donors());
        assertEquals(0, dashboard.getSummary().donations());

        donationService.recordDonation(DonationMethod.CASH, donor.getId(), typeId,
                new DonationRequest(100.0, null, null, 0, 0, null));
        DashboardSummary afterOne = dashboard.getSummary();
        assertEquals(1, afterOne.donations());
        assertEquals(100.0, afterOne.totalDonationValue(), 0.0001);

        donationService.recordDonation(DonationMethod.ONLINE, donor.getId(), typeId,
                new DonationRequest(50.0, "TX-1", null, 0, 0, null));
        donationService.recordDonation(DonationMethod.IN_KIND, donor.getId(), typeId,
                new DonationRequest(0, null, "Blankets", 5, 25.0, null));
        DashboardSummary afterThree = dashboard.getSummary();
        assertEquals(3, afterThree.donations());
        // 100 cash + 50 online + 25 estimated value of the in-kind gift
        assertEquals(175.0, afterThree.totalDonationValue(), 0.0001);
    }

    @Test
    void volunteerEventAndBeneficiaryCountsFollowTheirScreens() {
        int categoryId = new EventCategoryRepository(db).findAll().get(0).getId();

        assertTrue(volunteerController.saveVolunteer(null, "Omar Samy", "", "01198765432", "", "").success());
        assertTrue(eventController.saveEvent(null, "Winter drive", categoryId, "2026-12-15", "Cairo", "")
                .success());
        assertTrue(beneficiaryController.saveBeneficiary(null, "Hoda Mahmoud", "", "01098765432", "", "")
                .success());
        assertTrue(beneficiaryController.saveBeneficiary(null, "Sara Adel", "", "01012345678", "", "")
                .success());

        DashboardSummary summary = dashboard.getSummary();
        assertEquals(1, summary.volunteers());
        assertEquals(1, summary.events());
        assertEquals(2, summary.beneficiaries());
    }

    @Test
    void recentActivityComesFromTheLoggerNewestFirstAndIsLimited() {
        ApplicationLogger logger = ApplicationLogger.getInstance();
        for (int i = 1; i <= DashboardController.RECENT_ACTIVITY_LIMIT + 5; i++) {
            logger.info("dashboard test entry " + i);
        }

        var recent = dashboard.getSummary().recentActivity();

        assertEquals(DashboardController.RECENT_ACTIVITY_LIMIT, recent.size());
        assertTrue(recent.get(0).endsWith("dashboard test entry " + (DashboardController.RECENT_ACTIVITY_LIMIT + 5)),
                recent.get(0));
    }

    @Test
    void theActivityListCannotBeChangedByTheView() {
        DashboardSummary summary = dashboard.getSummary();

        assertThrows(UnsupportedOperationException.class, () -> summary.recentActivity().add("hack"));
    }

    @Test
    void aDatabaseProblemGivesZerosAndAnExplanationInsteadOfAnException() {
        // A database file that was never initialized has no tables, so every count fails.
        Database broken = new Database(tempDir.resolve("broken.db").toString());
        DashboardController brokenDashboard = new DashboardController(new DonorRepository(broken),
                new DonationRepository(broken, new DonationFactory()), new VolunteerRepository(broken),
                new EventRepository(broken), new BeneficiaryRepository(broken));

        DashboardSummary summary = brokenDashboard.getSummary();

        assertEquals(0, summary.donors());
        assertEquals(0.0, summary.totalDonationValue(), 0.0001);
        assertEquals(1, summary.recentActivity().size());
        assertTrue(summary.recentActivity().get(0).contains("database error"), summary.recentActivity().get(0));
    }
}
