package com.ripple.app;

import com.formdev.flatlaf.FlatLightLaf;
import com.ripple.communication.CommunicationService;
import com.ripple.communication.CommunicationStrategy;
import com.ripple.communication.EmailStrategy;
import com.ripple.communication.SmsStrategy;
import com.ripple.controller.BeneficiaryController;
import com.ripple.controller.CommunicationController;
import com.ripple.controller.DashboardController;
import com.ripple.controller.DonationController;
import com.ripple.controller.EventController;
import com.ripple.controller.LookupController;
import com.ripple.controller.VolunteerController;
import com.ripple.factory.CashDonationFactory;
import com.ripple.factory.ChequeDonationFactory;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.InKindDonationFactory;
import com.ripple.factory.OnlineDonationFactory;
import com.ripple.logging.ApplicationLogger;
import com.ripple.model.ContactChannel;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.EventCategory;
import com.ripple.observer.DonationLogObserver;
import com.ripple.observer.DonorNotificationObserver;
import com.ripple.observer.ReceiptObserver;
import com.ripple.repository.BeneficiaryRepository;
import com.ripple.repository.Database;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonationTypeRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.EventCategoryRepository;
import com.ripple.repository.EventRepository;
import com.ripple.repository.MessageLogRepository;
import com.ripple.repository.ReceiptRepository;
import com.ripple.repository.VolunteerRepository;
import com.ripple.service.DonationService;
import com.ripple.service.LookupService;
import com.ripple.view.BeneficiariesPanel;
import com.ripple.view.CommunicationPanel;
import com.ripple.view.DashboardPanel;
import com.ripple.view.DonationsPanel;
import com.ripple.view.EventsPanel;
import com.ripple.view.LookupPanel;
import com.ripple.view.MainFrame;
import com.ripple.view.VolunteersPanel;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.EnumMap;
import java.util.Map;

/**
 * Entry point of Ripple and its <b>composition root</b>: the one place that creates
 * the application's objects and wires them together.
 *
 * <p>Objects are created in this order and passed to each other through constructors
 * (see {@code docs/architecture.md}, "Composition root"):
 * <ol>
 *   <li>{@code Database}, then {@code initialize()} it;</li>
 *   <li>the donation creators (one {@code DonationFactory} per method), then the repositories;</li>
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
        useFlatLookAndFeel();

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

        // Section 2: donation creators, DonorRepository, DonationRepository, DonationService, DonationLogObserver
        // Factory Method: one Concrete Creator per method, keyed by the method it makes, so the
        // service picks a creator with factories.get(method) instead of an if/else chain.
        Map<DonationMethod, DonationFactory> donationFactories = new EnumMap<>(DonationMethod.class);
        DonationFactory[] creators = {new CashDonationFactory(), new OnlineDonationFactory(),
                new InKindDonationFactory(), new ChequeDonationFactory()};
        for (DonationFactory creator : creators) {
            donationFactories.put(creator.getMethod(), creator);
        }
        DonorRepository donorRepository = new DonorRepository(database);
        DonationRepository donationRepository = new DonationRepository(database, donationFactories);
        DonationService donationService = new DonationService(donationFactories, donorRepository,
                donationRepository, new DonationTypeRepository(database));
        // Observers: attached to the subject (DonationService).
        donationService.registerObserver(new DonationLogObserver());
        // Section 3: ReceiptRepository, ReceiptObserver
        ReceiptRepository receiptRepository = new ReceiptRepository(database);
        donationService.registerObserver(new ReceiptObserver(receiptRepository));
        // Section 4: VolunteerRepository, EventRepository, BeneficiaryRepository
        VolunteerRepository volunteerRepository = new VolunteerRepository(database);
        EventRepository eventRepository = new EventRepository(database);
        BeneficiaryRepository beneficiaryRepository = new BeneficiaryRepository(database);
        // Section 5: strategies map, MessageLogRepository, CommunicationService, DonorNotificationObserver
        Map<ContactChannel, CommunicationStrategy> strategies =
                Map.of(ContactChannel.EMAIL, new EmailStrategy(), ContactChannel.SMS, new SmsStrategy());
        MessageLogRepository messageLogRepository = new MessageLogRepository(database);
        CommunicationService communicationService = new CommunicationService(messageLogRepository);
        // Donor notification starts attached (CR5); the Donations screen checkbox can detach it.
        DonorNotificationObserver donorNotifier = new DonorNotificationObserver(communicationService, strategies);
        donationService.registerObserver(donorNotifier);

        // ---- Controllers ----
        // Section 1: LookupController
        LookupController lookupController = new LookupController(donationTypeService, eventCategoryService);
        // Section 2/3: DonationController
        DonationController donationController =
                new DonationController(donationService, receiptRepository, donorNotifier);
        // Section 4: VolunteerController, EventController, BeneficiaryController
        VolunteerController volunteerController = new VolunteerController(volunteerRepository);
        EventController eventController = new EventController(eventRepository, volunteerRepository);
        BeneficiaryController beneficiaryController = new BeneficiaryController(beneficiaryRepository);
        // Section 5: CommunicationController
        CommunicationController communicationController = new CommunicationController(communicationService,
                strategies, donorRepository, volunteerRepository, beneficiaryRepository, eventRepository,
                messageLogRepository);
        // Section 6: DashboardController (reads the five counting repositories and the logger)
        DashboardController dashboardController = new DashboardController(donorRepository, donationRepository,
                volunteerRepository, eventRepository, beneficiaryRepository);

        // Logged before the window is built, so it is always in the Dashboard's "Recent activity".
        ApplicationLogger.getInstance().info("Ripple started");

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
            frame.addScreen(MainFrame.VOLUNTEERS,
                    new VolunteersPanel(volunteerController, eventController, frame.getStatusBar()));
            frame.addScreen(MainFrame.EVENTS,
                    new EventsPanel(eventController, lookupController, frame.getStatusBar()));
            frame.addScreen(MainFrame.BENEFICIARIES,
                    new BeneficiariesPanel(beneficiaryController, frame.getStatusBar()));
            // Section 5: COMMUNICATION -> CommunicationPanel
            frame.addScreen(MainFrame.COMMUNICATION,
                    new CommunicationPanel(communicationController, eventController, frame.getStatusBar()));
            // Section 6: DASHBOARD     -> DashboardPanel
            frame.addScreen(MainFrame.DASHBOARD, new DashboardPanel(dashboardController));
            frame.setVisible(true);
        });
    }

    /**
     * Gives Ripple the modern FlatLaf look (the same on Windows and Linux) instead of Swing's
     * old default "Metal" theme, with rounded corners and a slightly larger font.
     */
    private static void useFlatLookAndFeel() {
        if (!FlatLightLaf.setup()) {
            // Swing's default look still works, so this is not fatal.
            ApplicationLogger.getInstance().error("Could not use the FlatLaf look and feel");
            return;
        }
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("defaultFont", UIManager.getFont("defaultFont").deriveFont(13f));
    }
}
