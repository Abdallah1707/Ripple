package com.ripple.controller;

import java.util.List;

/**
 * The data shown on the Dashboard: counts and recent log lines.
 */
public record DashboardSummary(int donors,
                               int donations,
                               double totalDonationValue,
                               int volunteers,
                               int events,
                               int beneficiaries,
                               List<String> recentActivity) {

    /** Copy the list so it can't be changed. */
    public DashboardSummary {
        recentActivity = List.copyOf(recentActivity);
    }
}
