/**
 * <b>Factory Method pattern</b>: the abstract Creator {@code DonationFactory} runs the common
 * workflow ({@code orderDonation}) and each Concrete Creator ({@code CashDonationFactory},
 * {@code OnlineDonationFactory}, {@code InKindDonationFactory}, {@code ChequeDonationFactory})
 * implements the factory method {@code createDonation} for its own kind of {@code Donation}.
 *
 * <p>Part of the Model in MVC. See {@code docs/class-diagram.md} section 5.4.
 */
package com.ripple.factory;
