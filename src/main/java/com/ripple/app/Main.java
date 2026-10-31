package com.ripple.app;

import com.ripple.controller.DonationController;
import com.ripple.controller.LookupController;
import com.ripple.factory.DonationFactory;
import com.ripple.logging.ApplicationLogger;
import com.ripple.model.DonationType;
import com.ripple.model.EventCategory;
import com.ripple.observer.DonationLogObserver;
import com.ripple.observer.DonorNotificationObserver;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.EventCategoryRepository;
import com.ripple.service.DonationService;
import com.ripple.service.LookupService;
import com.ripple.view.DonationsPanel;
import com.ripple.view.LookupPanel;
import com.ripple.view.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of Ripple and its <b>composition root</b>: the one place that creates
 * the application's objects and wires them together.
 *
 * <p>Objects are created in this order and passed to each other through constructors
 * (see {@code docs/architecture.md}, "Composition root"):
 * <ol>
 *   <li>{@code Database}, then {@code initialize()} it;</li>
 *   <li>{@code DonationFactory}, then the repositories;</li>
 *   <li>the communication strategies;</li>
 *   <li>the services;</li>
 *   <li>the observers, attached to {@code DonationService};</li>
 *   <li>the controllers;</li>
 *   <li>on the Swing thread: {@link MainFrame}, then each screen via
 *       {@link MainFrame#addScreen(String, javax.swing.JPanel)}.</li>
 * </ol>
 * Each section of {@code PHASE1.md} adds its lines at its own {@code // Section N} marker.
 */
public final class Main {

    private Main() {
        // Not meant to be instantiated.
    }

    /**
     * Starts Ripple.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        useNativeLookAndFeel();

        // ---- Model objects (database, repositories, services, observers) ----
        // Section 1: Database, lookup repositories and LookupServices
        Database database = Database.fromEnvironment();
        try {
            database.initialize();
        } catch (IllegalStateException e) {
            // Without a database nothing works, so explain the problem and stop.
            JOptionPane.showMessageDialog(null, "Ripple could not open its database.\n\n" + e.getMessage(),
                    "Ripple", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        LookupService<DonationType> donationTypeService =
                new LookupService<>(new DonationTypeRepository(database), "Donation type");
        LookupService<EventCategory> eventCategoryService =
                new LookupService<>(new EventCategoryRepository(database), "Event category");

        // Section 2: DonationFactory, DonorRepository, DonationRepository, DonationService, DonationLogObserver
        DonationFactory donationFactory = new DonationFactory();
        DonorRepository donorRepository = new DonorRepository(database);
        DonationRepository donationRepository = new DonationRepository(database, donationFactory);
        DonationService donationService = new DonationService(donationFactory, donorRepository,
                donationRepository, new DonationTypeRepository(database));
        // Observers: attached to the subject (DonationService). Donor notification starts attached (CR5).
        DonorNotificationObserver donorNotifier = new DonorNotificationObserver();
        donationService.attach(new DonationLogObserver());
        donationService.attach(donorNotifier);
        // Section 3: ReceiptRepository, ReceiptObserver
        // Section 4: VolunteerRepository, EventRepository, BeneficiaryRepository
        // Section 5: strategies map, MessageLogRepository, CommunicationService, DonorNotificationObserver

        // ---- Controllers ----
        // Section 1: LookupController
        LookupController lookupController = new LookupController(donationTypeService, eventCategoryService);
        // Section 2: DonationController
        DonationController donationController = new DonationController(donationService, donorNotifier);
        // Section 4: VolunteerController, EventController, BeneficiaryController
        // Section 5: CommunicationController
        // Section 6: DashboardController

        // Swing is not thread-safe: every UI object must be created on the
        // Event Dispatch Thread, which is what invokeLater does.
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            // ---- Screens: frame.addScreen(MainFrame.X, new XPanel(..., frame.getStatusBar())) ----
            // Section 1: LOOKUPS       -> LookupPanel
            frame.addScreen(MainFrame.LOOKUPS, new LookupPanel(lookupController, frame.getStatusBar()));
            // Section 2: DONATIONS     -> DonationsPanel
            frame.addScreen(MainFrame.DONATIONS,
                    new DonationsPanel(donationController, lookupController, frame.getStatusBar()));
            // Section 4: VOLUNTEERS, EVENTS, BENEFICIARIES
            // Section 5: COMMUNICATION -> CommunicationPanel
            // Section 6: DASHBOARD     -> DashboardPanel
            frame.setVisible(true);
        });
        ApplicationLogger.getInstance().info("Ripple started");
    }

    /** Makes Ripple look like a normal Windows (or Linux) app instead of Swing's default "Metal" theme. */
    private static void useNativeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // The default look and feel still works, so this is not fatal.
            System.err.println("Could not use the system look and feel: " + e.getMessage());
        }
    }
}
