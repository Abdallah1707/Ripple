package com.ripple.controller;

import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.logging.ApplicationLogger;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.Donor;
import com.ripple.observer.DonorNotificationObserver;
import com.ripple.repository.ReceiptRepository;
import com.ripple.service.DonationService;
import com.ripple.service.InputValidator;
import com.ripple.service.ValidationException;

import java.util.List;

/**
 * Controller for the Donations screen.
 *
 * <p>It turns the texts typed on the screen into typed values, passes them to
 * {@link DonationService}, and turns the outcome into a {@link Result} for the status bar.
 * It never throws to the view. It also attaches / detaches the donor notification observer
 * when the "Notify donors on new donation" checkbox changes, and looks up a donation's
 * saved receipt for the "View receipt" pop-up.
 */
public class DonationController {

    private final DonationService donationService;
    private final ReceiptRepository receiptRepository;
    private final DonorNotificationObserver donorNotifier;

    /**
     * Creates the controller.
     *
     * @param donationService   the donation service (the Observer subject)
     * @param receiptRepository where saved receipts are read from
     * @param donorNotifier     the observer that the checkbox attaches and detaches
     */
    public DonationController(DonationService donationService, ReceiptRepository receiptRepository,
                              DonorNotificationObserver donorNotifier) {
        this.donationService = donationService;
        this.receiptRepository = receiptRepository;
        this.donorNotifier = donorNotifier;
    }

    /**
     * Returns all donors, e.g. for the donor combo box.
     *
     * @return the donors sorted by name
     */
    public List<Donor> listDonors() {
        return donationService.findAllDonors();
    }

    /**
     * Registers a new donor.
     *
     * @param name    the name typed by the user
     * @param email   the email typed by the user
     * @param phone   the phone typed by the user
     * @param channel {@code "EMAIL"} or {@code "SMS"}
     * @return what happened, for the status bar
     */
    public Result registerDonor(String name, String email, String phone, String channel) {
        try {
            Donor donor = donationService.registerDonor(name, email, phone, parseChannel(channel));
            String message = "Donor '" + donor.getName() + "' registered";
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not save the donor because of a database error. Details are in logs/ripple.log.");
        }
    }

    /**
     * Records a donation. Only the texts that belong to the chosen method are read; the
     * others can be empty.
     *
     * @param donorId          id of the chosen donor
     * @param typeId           id of the chosen donation type
     * @param method           {@code "CASH"}, {@code "ONLINE"} or {@code "IN_KIND"}
     * @param amount           amount in EGP (cash, online)
     * @param transactionRef   payment reference (online)
     * @param item             what was given (in-kind)
     * @param quantity         how many (in-kind)
     * @param estimatedValue   estimated value in EGP (in-kind)
     * @return what happened, for the status bar
     */
    public Result recordDonation(int donorId, int typeId, String method, String amount,
                                 String transactionRef, String item, String quantity, String estimatedValue) {
        try {
            DonationMethod donationMethod = new DonationFactory().parseMethod(method);
            DonationRequest request = switch (donationMethod) {
                case CASH -> new DonationRequest(
                        InputValidator.parseDouble(amount, "Amount"), null, null, 0, 0, null);
                case ONLINE -> new DonationRequest(
                        InputValidator.parseDouble(amount, "Amount"), transactionRef, null, 0, 0, null);
                case IN_KIND -> new DonationRequest(0, null, item,
                        InputValidator.parseInt(quantity, "Quantity"),
                        InputValidator.parseDouble(estimatedValue, "Estimated value"), null);
            };
            Donation donation = donationService.recordDonation(donationMethod, donorId, typeId, request);
            return Result.ok("Donation " + donation.getReceiptNumber() + " recorded");
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not save the donation because of a database error. Details are in logs/ripple.log.");
        }
    }

    /**
     * Returns all donations, newest first, for the table.
     *
     * @return the donations
     */
    public List<Donation> listDonations() {
        return donationService.findAllDonations();
    }

    /**
     * Returns the saved receipt text for a donation, for the "View receipt" pop-up.
     *
     * @param donationId id of the donation
     * @return the receipt text, or a message saying none was saved yet
     */
    public String getReceipt(int donationId) {
        return receiptRepository.findByDonationId(donationId)
                .orElse("No receipt was saved for this donation yet.");
    }

    /**
     * Turns the automatic donor thank-you on or off (the checkbox on the Donations screen).
     * This attaches or detaches the {@link DonorNotificationObserver}.
     *
     * @param enabled {@code true} to attach the observer, {@code false} to detach it
     */
    public void setDonorNotifications(boolean enabled) {
        if (enabled) {
            donationService.attach(donorNotifier);
        } else {
            donationService.detach(donorNotifier);
        }
    }

    /**
     * Tells whether donors are currently notified about new donations.
     *
     * @return {@code true} if the donor notification observer is attached
     */
    public boolean isDonorNotificationsEnabled() {
        return donationService.isAttached(donorNotifier);
    }

    /** Converts {@code "EMAIL"} / {@code "SMS"} to a channel, or explains what is wrong. */
    private ContactChannel parseChannel(String channel) {
        if (channel != null) {
            for (ContactChannel value : ContactChannel.values()) {
                if (value.name().equalsIgnoreCase(channel.trim())) {
                    return value;
                }
            }
        }
        throw new ValidationException("Preferred channel must be Email or SMS");
    }
}
