package com.ripple.controller;

import com.ripple.logging.ApplicationLogger;
import com.ripple.repository.BeneficiaryRepository;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.EventRepository;
import com.ripple.repository.VolunteerRepository;

import java.util.List;

/**
 * Controller for the Dashboard screen.
 * Gets the counts from the repositories and the latest log lines.
 */
public class DashboardController {

    /** How many log lines to show. */
    public static final int RECENT_ACTIVITY_LIMIT = 10;

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final VolunteerRepository volunteerRepository;
    private final EventRepository eventRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final ApplicationLogger logger = ApplicationLogger.getInstance();

    /** Creates the controller. */
    public DashboardController(DonorRepository donorRepository, DonationRepository donationRepository,
                               VolunteerRepository volunteerRepository, EventRepository eventRepository,
                               BeneficiaryRepository beneficiaryRepository) {
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.volunteerRepository = volunteerRepository;
        this.eventRepository = eventRepository;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    /** Gets the current totals and recent activity. */
    public DashboardSummary getSummary() {
        try {
            return new DashboardSummary(
                    donorRepository.count(),
                    donationRepository.count(),
                    donationRepository.totalValue(),
                    volunteerRepository.count(),
                    eventRepository.count(),
                    beneficiaryRepository.count(),
                    logger.getRecentEntries(RECENT_ACTIVITY_LIMIT));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            return new DashboardSummary(0, 0, 0, 0, 0, 0,
                    List.of("Could not load the totals because of a database error. "
                            + "Details are in logs/ripple.log."));
        }
    }
}
