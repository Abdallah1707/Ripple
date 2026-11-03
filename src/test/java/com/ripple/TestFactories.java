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
 * Test helper: the same creators map that {@code Main} builds (one Factory Method creator per
 * donation method), so tests wire services and repositories exactly like the app.
 *
 * <pre>{@code
 * DonationRepository repository = new DonationRepository(database, TestFactories.all());
 * Donation cash = TestFactories.create(DonationMethod.CASH, donor, type, request);
 * }</pre>
 */
public final class TestFactories {

    private TestFactories() {
    }

    /**
     * Returns a new map with one creator for every donation method.
     *
     * @return method → creator
     */
    public static Map<DonationMethod, DonationFactory> all() {
        Map<DonationMethod, DonationFactory> factories = new EnumMap<>(DonationMethod.class);
        DonationFactory[] creators = {new CashDonationFactory(), new OnlineDonationFactory(),
                new InKindDonationFactory(), new ChequeDonationFactory()};
        for (DonationFactory creator : creators) {
            factories.put(creator.getMethod(), creator);
        }
        return factories;
    }

    /**
     * Creates a donation through the creator for {@code method}.
     *
     * @param method  how it was given
     * @param donor   who gave it
     * @param type    its donation type
     * @param request the details
     * @return the new (unsaved) donation
     */
    public static Donation create(DonationMethod method, Donor donor, DonationType type, DonationRequest request) {
        return all().get(method).orderDonation(donor, type, request);
    }
}
