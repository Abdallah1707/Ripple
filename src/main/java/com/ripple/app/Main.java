package com.ripple.app;

import com.formdev.flatlaf.FlatLightLaf;
import com.ripple.communication.CommunicationService;
import com.ripple.communication.CommunicationStrategy;
import com.ripple.communication.EmailStrategy;
import com.ripple.communication.SmsStrategy;
import com.ripple.communication.SocialMediaStrategy;
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
import com.ripple.observer.AttendeeNotificationObserver;
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
import com.ripple.service.EventService;
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
import java.util.List;
import java.util.Map;

/**
 * Starts the app and creates all the main objects.
 * Order: database, factories and repositories, strategies, services, observers,
 * controllers, then the window and its screens.
 */
public final class Main {

    private Main() {
        // no instances
    }

    /** Starts Ripple. */
    public static void main(String[] args) {
        useFlatLookAndFeel();

        // ---- Model ----
        // Section 1: database and lookups
        Database database = Database.fromEnvironment();
        try {
            database.initialize();
        } catch (IllegalStateException e) {
            // can't run without the database, so show the error and quit
            JOptionPane.showMessageDialog(null, "Ripple could not open its database.\n\n" + e.getMessage(),
                    "Ripple", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        LookupService<DonationType> donationTypeService =
                new LookupService<>(new DonationTypeRepository(database), "Donation type");
        LookupService<EventCategory> eventCategoryService =
                new LookupService<>(new EventCategoryRepository(database), "Event category");

        // Section 2: donations
        // one factory per donation method
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
        // observers
        donationService.registerObserver(new DonationLogObserver());
        // Section 3: receipts
        ReceiptRepository receiptRepository = new ReceiptRepository(database);
        donationService.registerObserver(new ReceiptObserver(receiptRepository));
        // Section 4: volunteers, events, beneficiaries
        VolunteerRepository volunteerRepository = new VolunteerRepository(database);
        EventRepository eventRepository = new EventRepository(database);
        EventService eventService = new EventService(eventRepository);
        BeneficiaryRepository beneficiaryRepository = new BeneficiaryRepository(database);
        // Section 5: communication
        Map<ContactChannel, CommunicationStrategy> strategies =
                Map.of(ContactChannel.EMAIL, new EmailStrategy(), ContactChannel.SMS, new SmsStrategy(),
                        ContactChannel.SOCIAL, new SocialMediaStrategy());
        MessageLogRepository messageLogRepository = new MessageLogRepository(database);
        CommunicationService communicationService = new CommunicationService(messageLogRepository);
        // donor thank-you is on by default, the checkbox can turn it off
        DonorNotificationObserver donorNotifier = new DonorNotificationObserver(communicationService, strategies);
        donationService.registerObserver(donorNotifier);
        // tell attendees when an event's date or location changes
        // it has its own CommunicationService so it doesn't change the one the screen uses
        eventService.registerObserver(new AttendeeNotificationObserver(eventRepository,
                new CommunicationService(messageLogRepository),
                List.of(strategies.get(ContactChannel.EMAIL), strategies.get(ContactChannel.SMS))));

        // ---- Controllers ----
        // Section 1
        LookupController lookupController = new LookupController(donationTypeService, eventCategoryService);
        // Section 2/3
        DonationController donationController =
                new DonationController(donationService, receiptRepository, donorNotifier);
        // Section 4
        VolunteerController volunteerController = new VolunteerController(volunteerRepository);
        EventController eventController = new EventController(eventService, eventRepository, volunteerRepository);
        BeneficiaryController beneficiaryController = new BeneficiaryController(beneficiaryRepository);
        // Section 5
        CommunicationController communicationController = new CommunicationController(communicationService,
                strategies, donorRepository, volunteerRepository, beneficiaryRepository, eventRepository,
                messageLogRepository);
        // Section 6
        DashboardController dashboardController = new DashboardController(donorRepository, donationRepository,
                volunteerRepository, eventRepository, beneficiaryRepository);

        // log this before the window opens so it shows on the dashboard
        ApplicationLogger.getInstance().info("Ripple started");

        // Swing objects have to be created on the UI thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            // ---- Screens ----
            // Section 1
            frame.addScreen(MainFrame.LOOKUPS, new LookupPanel(lookupController, frame.getStatusBar()));
            // Section 2
            frame.addScreen(MainFrame.DONATIONS,
                    new DonationsPanel(donationController, lookupController, frame.getStatusBar()));
            // Section 4
            frame.addScreen(MainFrame.VOLUNTEERS,
                    new VolunteersPanel(volunteerController, eventController, frame.getStatusBar()));
            frame.addScreen(MainFrame.EVENTS,
                    new EventsPanel(eventController, lookupController, frame.getStatusBar()));
            frame.addScreen(MainFrame.BENEFICIARIES,
                    new BeneficiariesPanel(beneficiaryController, frame.getStatusBar()));
            // Section 5
            frame.addScreen(MainFrame.COMMUNICATION,
                    new CommunicationPanel(communicationController, eventController, frame.getStatusBar()));
            // Section 6
            frame.addScreen(MainFrame.DASHBOARD, new DashboardPanel(dashboardController));
            frame.setVisible(true);
        });
    }

    /** Uses the FlatLaf theme with a slightly bigger font. */
    private static void useFlatLookAndFeel() {
        if (!FlatLightLaf.setup()) {
            // the default Swing look still works
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
