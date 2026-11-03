package com.ripple.service;

import com.ripple.extras.DonationOptions;
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
import java.util.Map;

/**
 * Rules for donors and donations. Also the observer subject: after a donation
 * is saved it notifies all observers. Uses the factories to create donations.
 */
public class DonationService implements DonationSubject {

    private final List<DonationObserver> observers = new ArrayList<>();
    private final Map<DonationMethod, DonationFactory> factories;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final LookupRepository<DonationType> donationTypeRepository;

    /** Creates the service. */
    public DonationService(Map<DonationMethod, DonationFactory> factories, DonorRepository donorRepository,
                           DonationRepository donationRepository,
                           LookupRepository<DonationType> donationTypeRepository) {
        this.factories = factories;
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.donationTypeRepository = donationTypeRepository;
    }

    // ---- observer methods ----

    @Override
    public void registerObserver(DonationObserver observer) {
        if (observer != null && !observers.contains(observer)) { // no duplicates
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(DonationObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Donation donation) {
        // loop over a copy in case an observer removes itself
        for (DonationObserver observer : new ArrayList<>(observers)) {
            try {
                observer.update(donation);
            } catch (RuntimeException e) {
                // one failing observer shouldn't stop the rest
                ApplicationLogger.getInstance().error(
                        observer.getClass().getSimpleName() + " failed: " + e.getMessage());
            }
        }
    }

    /** True if the observer is registered. */
    public boolean isRegistered(DonationObserver observer) {
        return observers.contains(observer);
    }

    // ---- donors ----

    /** Checks and saves a new donor. Throws ValidationException if something is wrong. */
    public Donor registerDonor(String name, String email, String phone, String socialHandle,
                               ContactChannel channel) {
        return donorRepository.insert(validateDonor(0, name, email, phone, socialHandle, channel));
    }

    /** Checks and updates a donor. Same rules as registerDonor. */
    public Donor updateDonor(int id, String name, String email, String phone, String socialHandle,
                             ContactChannel channel) {
        requireExistingDonor(id);
        Donor donor = validateDonor(id, name, email, phone, socialHandle, channel);
        donorRepository.update(donor);
        return donor;
    }

    /** Deletes a donor. Donors with donations can't be deleted. */
    public Donor deleteDonor(int id) {
        Donor donor = requireExistingDonor(id);
        int donations = donorRepository.countDonations(id);
        if (donations > 0) {
            throw new ValidationException("'" + donor.getName() + "' has " + donations
                    + " donation(s) and cannot be deleted");
        }
        donorRepository.delete(id);
        return donor;
    }

    private Donor requireExistingDonor(int id) {
        return donorRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Please choose a donor"));
    }

    /** Donor checks used by register and update. */
    private static Donor validateDonor(int id, String name, String email, String phone, String socialHandle,
                                       ContactChannel channel) {
        String cleanName = InputValidator.requireText(name, "Donor name", 1, 100);
        String cleanEmail = email == null ? "" : email.trim();
        String cleanPhone = phone == null ? "" : phone.trim();
        String cleanHandle = socialHandle == null ? "" : socialHandle.trim();
        if (channel == null) {
            throw new ValidationException("Preferred channel is required");
        }
        // phone is optional for email donors but must be 11 digits if typed
        if (!cleanPhone.isEmpty() && !cleanPhone.replaceAll("[\\s-]", "").matches("\\d{11}")) {
            throw new ValidationException("Phone number must be exactly 11 digits");
        }
        if (channel == ContactChannel.EMAIL && !InputValidator.isValidEmail(cleanEmail)) {
            throw new ValidationException("A valid email is required when the donor prefers Email");
        }
        if (channel == ContactChannel.SMS && !InputValidator.isValidPhone(cleanPhone)) {
            throw new ValidationException("A valid phone number is required when the donor prefers SMS");
        }
        // handle is optional for other channels but must be valid if typed
        if (!cleanHandle.isEmpty() && !InputValidator.isValidSocialHandle(cleanHandle)) {
            throw new ValidationException("Social handle must start with @ (example: @mona_ali)");
        }
        if (channel == ContactChannel.SOCIAL && cleanHandle.isEmpty()) {
            throw new ValidationException("A social media handle is required when the donor prefers Social");
        }
        return new Donor(id, cleanName, cleanEmail, cleanPhone, cleanHandle, channel);
    }

    /** All donors sorted by name. */
    public List<Donor> findAllDonors() {
        return donorRepository.findAll();
    }

    // ---- donations ----

    /** Records a donation with no extras. */
    public Donation recordDonation(DonationMethod method, int donorId, int donationTypeId,
                                   DonationRequest request) {
        return recordDonation(method, donorId, donationTypeId, request, DonationOptions.NONE);
    }

    /**
     * Checks the donation, saves it, then notifies the observers.
     * Nothing is saved if something is invalid.
     */
    public Donation recordDonation(DonationMethod method, int donorId, int donationTypeId,
                                   DonationRequest request, DonationOptions options) {
        if (options == null) {
            throw new ValidationException("Donation options are required");
        }
        if (options.isGift()) {
            InputValidator.requireText(options.honoree(), "In honor of", 2, 100);
        }
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ValidationException("Please choose a donor"));
        DonationType type = donationTypeRepository.findById(donationTypeId)
                .orElseThrow(() -> new ValidationException("Please choose a donation type"));

        if (method == null) {
            throw new ValidationException("Donation method is required");
        }
        DonationFactory factory = factories.get(method); // factory for this method
        if (factory == null) {
            throw new ValidationException("Unsupported donation method: " + method.getLabel());
        }
        Donation base = factory.orderDonation(donor, type, request); // checks the details
        Donation donation = options.decorate(base);                  // add the extras
        donationRepository.insert(donation);
        notifyObservers(donation);
        return donation;
    }

    /** All donations, newest first. */
    public List<Donation> findAllDonations() {
        return donationRepository.findAll();
    }
}
