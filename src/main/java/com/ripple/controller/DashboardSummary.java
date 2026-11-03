package com.ripple.controller;

import java.util.List;

/**
 * Everything the Dashboard screen shows, in one object: the totals for each module and
 * the latest lines of the application log.
 *
 * <p>It is a plain data carrier between {@link DashboardController} and the Dashboard view,
 * so the view never has to talk to a repository to get its numbers.
 *
 * @param donors             number of registered donors
 * @param donations          number of recorded donations
 * @param totalDonationValue value of all donations in EGP (money amounts plus in-kind estimates)
 * @param volunteers         number of volunteers
 * @param events             number of events
 * @param beneficiaries      number of beneficiaries
 * @param recentActivity     the newest log entries, newest first
 */
public record DashboardSummary(int donors,
                               int donations,
                               double totalDonationValue,
                               int volunteers,
                               int events,
                               int beneficiaries,
                               List<String> recentActivity) {

    /** Keeps an unmodifiable copy, so a view can never change the controller's list. */
    public DashboardSummary {
        recentActivity = List.copyOf(recentActivity);
    }
}
