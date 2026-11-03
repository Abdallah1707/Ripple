package com.ripple.controller;

import com.ripple.extras.DonationOptions;
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
import java.util.Locale;

/**
 * Controller for the Donations screen.
 * Also turns the donor notification observer on or off.
 */
public class DonationController {

    private final DonationService donationService;
    private final ReceiptRepository receiptRepository;
    private final DonorNotificationObserver donorNotifier;

    /** Creates the controller. */
    public DonationController(DonationService donationService, ReceiptRepository receiptRepository,
                              DonorNotificationObserver donorNotifier) {
        this.donationService = donationService;
        this.receiptRepository = receiptRepository;
        this.donorNotifier = donorNotifier;
    }

    /** All donors sorted by name. */
    public List<Donor> listDonors() {
        return donationService.findAllDonors();
    }

    /** Adds a donor if id is null, otherwise updates it. */
    public Result saveDonor(Integer id, String name, String email, String phone, String socialHandle,
                            String channel) {
        boolean adding = id == null;
        try {
            ContactChannel preferred = parseChannel(channel);
            Donor donor = adding
                    ? donationService.registerDonor(name, email, phone, socialHandle, preferred)
                    : donationService.updateDonor(id, name, email, phone, socialHandle, preferred);
            String message = "Donor '" + donor.getName() + "' " + (adding ? "registered" : "updated");
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not save the donor because of a database error. Details are in logs/ripple.log.");
        }
    }

    /** Deletes a donor. Donors with donations can't be deleted. */
    public Result deleteDonor(int id) {
        try {
            Donor donor = donationService.deleteDonor(id);
            String message = "Donor '" + donor.getName() + "' deleted";
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not delete the donor because of a database error. Details are in logs/ripple.log.");
        }
    }

    /** Records a donation. Only the fields for the chosen method are used. */
    public Result recordDonation(int donorId, int typeId, String method, String amount,
                                 String transactionRef, String item, String quantity, String estimatedValue) {
        return recordDonation(donorId, typeId, method, amount, transactionRef, item, quantity, estimatedValue,
                false, "", false);
    }

    /** Records a donation with extras (urgent, gift, anonymous). */
    public Result recordDonation(int donorId, int typeId, String method, String amount,
                                 String transactionRef, String item, String quantity, String estimatedValue,
                                 boolean urgent, String honoree, boolean anonymous) {
        try {
            DonationMethod donationMethod = parseMethod(method);
            DonationRequest request = switch (donationMethod) {
                case CASH -> new DonationRequest(
                        InputValidator.parseDouble(amount, "Amount"), null, null, 0, 0, null);
                case ONLINE -> new DonationRequest(
                        InputValidator.parseDouble(amount, "Amount"), transactionRef, null, 0, 0, null);
                case IN_KIND -> new DonationRequest(0, null, item,
                        InputValidator.parseInt(quantity, "Quantity"),
                        InputValidator.parseDouble(estimatedValue, "Estimated value"), null);
                case CHEQUE -> new DonationRequest(
                        InputValidator.parseDouble(amount, "Amount"), transactionRef, null, 0, 0, null);
            };
            Donation donation = donationService.recordDonation(donationMethod, donorId, typeId, request,
                    new DonationOptions(urgent, honoree, anonymous));
            String extras = donation.getOptions().isEmpty() ? "" : " (" + String.join(", ", donation.getOptions()) + ")";
            return Result.ok("Donation " + donation.getReceiptNumber() + " recorded" + extras);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not save the donation because of a database error. Details are in logs/ripple.log.");
        }
    }

    /** All donations, newest first. */
    public List<Donation> listDonations() {
        return donationService.findAllDonations();
    }

    /** Gets the saved receipt for a donation. */
    public String getReceipt(int donationId) {
        return receiptRepository.findByDonationId(donationId)
                .orElse("No receipt was saved for this donation yet.");
    }

    /** Turns the donor thank-you messages on or off. */
    public void setDonorNotifications(boolean enabled) {
        if (enabled) {
            donationService.registerObserver(donorNotifier);
        } else {
            donationService.removeObserver(donorNotifier);
        }
    }

    /** True if donor thank-you messages are on. */
    public boolean isDonorNotificationsEnabled() {
        return donationService.isRegistered(donorNotifier);
    }

    /** Turns a method name like "online" or "In-kind" into a DonationMethod. */
    private DonationMethod parseMethod(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Donation method is required");
        }
        String key = name.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return DonationMethod.valueOf(key);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unsupported donation method: " + name.trim());
        }
    }

    /** Turns "EMAIL" / "SMS" / "SOCIAL" into a channel. */
    private ContactChannel parseChannel(String channel) {
        if (channel != null) {
            for (ContactChannel value : ContactChannel.values()) {
                if (value.name().equalsIgnoreCase(channel.trim())) {
                    return value;
                }
            }
        }
        throw new ValidationException("Preferred channel must be Email, SMS or Social");
    }
}
