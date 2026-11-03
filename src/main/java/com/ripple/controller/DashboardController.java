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
 *
 * <p>It asks each module's repository for its count, adds the latest entries of the
 * {@link ApplicationLogger} (the Singleton), and hands everything to the view as one
 * {@link DashboardSummary}. This is why the Dashboard view needs no repository of its own.
 *
 * <p>Like every controller it never throws to the view: if the database cannot be read,
 * the problem is logged and the Dashboard shows zeros and an explanation.
 */
public class DashboardController {

    /** How many log lines the Dashboard shows under "Recent activity". */
    public static final int RECENT_ACTIVITY_LIMIT = 10;

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final VolunteerRepository volunteerRepository;
    private final EventRepository eventRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final ApplicationLogger logger = ApplicationLogger.getInstance();

    /**
     * Creates the controller.
     *
     * @param donorRepository       where the donor count comes from
     * @param donationRepository    where the donation count and total value come from
     * @param volunteerRepository   where the volunteer count comes from
     * @param eventRepository       where the event count comes from
     * @param beneficiaryRepository where the beneficiary count comes from
     */
    public DashboardController(DonorRepository donorRepository, DonationRepository donationRepository,
                               VolunteerRepository volunteerRepository, EventRepository eventRepository,
                               BeneficiaryRepository beneficiaryRepository) {
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.volunteerRepository = volunteerRepository;
        this.eventRepository = eventRepository;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    /**
     * Collects the current totals and the latest activity.
     *
     * @return the numbers and log lines to show; never {@code null}
     */
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
