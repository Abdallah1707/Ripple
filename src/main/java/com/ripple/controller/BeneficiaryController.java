package com.ripple.controller;

import com.ripple.logging.ApplicationLogger;
import com.ripple.model.Beneficiary;
import com.ripple.repository.BeneficiaryRepository;
import com.ripple.service.InputValidator;
import com.ripple.service.ValidationException;

import java.util.List;

/**
 * Controller for the Beneficiaries screen.
 */
public class BeneficiaryController {

    private final BeneficiaryRepository beneficiaryRepository;

    /** Creates the controller. */
    public BeneficiaryController(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    /** All beneficiaries sorted by name. */
    public List<Beneficiary> listBeneficiaries() {
        return beneficiaryRepository.findAll();
    }

    /** Adds a beneficiary if id is null, otherwise updates it. */
    public Result saveBeneficiary(Integer id, String name, String email, String phone, String address,
                                  String needs) {
        boolean adding = id == null;
        try {
            String cleanName = InputValidator.requireText(name, "Name", 1, 100);
            Beneficiary beneficiary = new Beneficiary(adding ? 0 : id, cleanName,
                    InputValidator.optionalEmail(email), InputValidator.optionalPhone(phone),
                    clean(address), clean(needs));
            if (adding) {
                beneficiaryRepository.insert(beneficiary);
            } else {
                beneficiaryRepository.update(beneficiary);
            }
            String message = "Beneficiary '" + cleanName + "' " + (adding ? "added" : "updated");
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not save the beneficiary because of a database error. "
                    + "Details are in logs/ripple.log.");
        }
    }

    /** Deletes a beneficiary. */
    public Result deleteBeneficiary(int id) {
        try {
            String name = beneficiaryRepository.findAll().stream().filter(b -> b.getId() == id)
                    .map(Beneficiary::getName).findFirst().orElse("");
            beneficiaryRepository.delete(id);
            String message = "Beneficiary '" + name + "' deleted";
            ApplicationLogger.getInstance().info(message);
            return Result.ok(message);
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not delete the beneficiary because of a database error. "
                    + "Details are in logs/ripple.log.");
        }
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }
}
