package com.ripple;

import com.ripple.factory.CashDonationFactory;
import com.ripple.factory.ChequeDonationFactory;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.factory.InKindDonationFactory;
import com.ripple.factory.OnlineDonationFactory;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Test helper with the same factories map that Main builds.
 */
public final class TestFactories {

    private TestFactories() {
    }

    /** A new map with a factory for every method. */
    public static Map<DonationMethod, DonationFactory> all() {
        Map<DonationMethod, DonationFactory> factories = new EnumMap<>(DonationMethod.class);
        DonationFactory[] creators = {new CashDonationFactory(), new OnlineDonationFactory(),
                new InKindDonationFactory(), new ChequeDonationFactory()};
        for (DonationFactory creator : creators) {
            factories.put(creator.getMethod(), creator);
        }
        return factories;
    }

    /** Creates a donation with the factory for the method. */
    public static Donation create(DonationMethod method, Donor donor, DonationType type, DonationRequest request) {
        return all().get(method).orderDonation(donor, type, request);
    }
}
