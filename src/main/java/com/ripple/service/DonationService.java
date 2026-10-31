package com.ripple.service;

import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.logging.ApplicationLogger;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.observer.DonationObserver;
import com.ripple.observer.DonationSubject;
import com.ripple.repository.DonationRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.LookupRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for donors and donations. Also the <b>Subject</b> in the Observer pattern:
 * after a donation is saved it tells every attached {@link DonationObserver}.
 *
 * <p>This class does not know what the observers do (log, receipt, thank-you message...).
 * That is the point of the pattern: new reactions can be added without changing this class.
 * It uses {@link DonationFactory} (Factory pattern) to create and validate donations.
 */
public class DonationService implements DonationSubject {

    private final List<DonationObserver> observers = new ArrayList<>();
    private final DonationFactory factory;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final LookupRepository<DonationType> donationTypeRepository;

    /**
     * Creates the service.
     *
     * @param factory                creates and validates donations
     * @param donorRepository        donor storage
     * @param donationRepository     donation storage
     * @param donationTypeRepository donation type storage (to find the chosen type)
     */
    public DonationService(DonationFactory factory, DonorRepository donorRepository,
                           DonationRepository donationRepository,
                           LookupRepository<DonationType> donationTypeRepository) {
        this.factory = factory;
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.donationTypeRepository = donationTypeRepository;
    }

    // ---------------- Observer pattern: the Subject's methods ----------------

    @Override
    public void attach(DonationObserver observer) {
        if (observer != null && !observers.contains(observer)) { // attaching twice has no effect
            observers.add(observer);
        }
    }

    @Override
    public void detach(DonationObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Donation donation) {
        // Loop over a copy so an observer may detach itself while being notified.
        for (DonationObserver observer : new ArrayList<>(observers)) {
            try {
                observer.onDonationRecorded(donation);
            } catch (RuntimeException e) {
                // One failing observer must not stop the others from running.
                ApplicationLogger.getInstance().error(
                        observer.getClass().getSimpleName() + " failed: " + e.getMessage());
            }
        }
    }

    /**
     * Tells whether an observer is currently attached.
     *
     * @param observer the observer to look for
     * @return {@code true} if it is attached
     */
    public boolean isAttached(DonationObserver observer) {
        return observers.contains(observer);
    }

    // ---------------- Donors ----------------

    /**
     * Checks and saves a new donor.
     *
     * @param name    the donor's name (required)
     * @param email   the email address (required if the donor prefers Email)
     * @param phone   the phone number (required if the donor prefers SMS)
     * @param channel the preferred channel for thank-you messages (CR5)
     * @return the saved donor, with its id
     * @throws ValidationException if a rule is broken
     */
    public Donor registerDonor(String name, String email, String phone, ContactChannel channel) {
        String cleanName = InputValidator.requireText(name, "Donor name", 1, 100);
        String cleanEmail = email == null ? "" : email.trim();
        String cleanPhone = phone == null ? "" : phone.trim();
        if (channel == null) {
            throw new ValidationException("Preferred channel is required");
        }
        // A phone number is optional for Email donors, but if one is typed it must be exactly 11 digits.
        if (!cleanPhone.isEmpty() && !cleanPhone.replaceAll("[\\s-]", "").matches("\\d{11}")) {
            throw new ValidationException("Phone number must be exactly 11 digits");
        }
        if (channel == ContactChannel.EMAIL && !InputValidator.isValidEmail(cleanEmail)) {
            throw new ValidationException("A valid email is required when the donor prefers Email");
        }
        if (channel == ContactChannel.SMS && !InputValidator.isValidPhone(cleanPhone)) {
            throw new ValidationException("A valid phone number is required when the donor prefers SMS");
        }
        return donorRepository.insert(new Donor(0, cleanName, cleanEmail, cleanPhone, channel));
    }

    /**
     * Returns all donors sorted by name.
     *
     * @return all donors
     */
    public List<Donor> findAllDonors() {
        return donorRepository.findAll();
    }

    // ---------------- Donations ----------------

    /**
     * Records a donation: checks it, saves it, then tells the observers.
     * If anything is invalid nothing is saved and no observer is told.
     *
     * @param method         how the donation was given
     * @param donorId        id of the donor
     * @param donationTypeId id of the donation type
     * @param request        the details for that method
     * @return the saved donation, with its id and receipt number
     * @throws ValidationException if the donor, type or details are not valid
     */
    public Donation recordDonation(DonationMethod method, int donorId, int donationTypeId,
                                   DonationRequest request) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ValidationException("Please choose a donor"));
        DonationType type = donationTypeRepository.findById(donationTypeId)
                .orElseThrow(() -> new ValidationException("Please choose a donation type"));

        Donation donation = factory.create(method, donor, type, request); // validates the details
        donationRepository.insert(donation);
        notifyObservers(donation);
        return donation;
    }

    /**
     * Returns all donations, newest first.
     *
     * @return all donations
     */
    public List<Donation> findAllDonations() {
        return donationRepository.findAll();
    }
}
