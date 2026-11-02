package com.ripple.controller;

import com.ripple.TestDatabase;
import com.ripple.model.Beneficiary;
import com.ripple.repository.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the Beneficiaries screen's rules: validation and add / edit / delete. */
class BeneficiaryControllerTest {

    @TempDir
    Path tempDir;

    private BeneficiaryController controller;

    @BeforeEach
    void setUp() {
        controller = new BeneficiaryController(new BeneficiaryRepository(TestDatabase.create(tempDir)));
    }

    @Test
    void addEditAndDeleteWork() {
        assertTrue(controller.saveBeneficiary(null, "Hoda Mahmoud", "", "01098765432", "12 Nile St",
                "Food parcels").success());
        Beneficiary saved = controller.listBeneficiaries().get(0);
        assertEquals("12 Nile St", saved.getAddress());
        assertEquals("Food parcels", saved.getNeeds());

        assertTrue(controller.saveBeneficiary(saved.getId(), "Hoda Mahmoud", "", "01098765432", "5 Giza Rd",
                "School fees").success());
        Beneficiary reloaded = controller.listBeneficiaries().get(0);
        assertEquals("5 Giza Rd", reloaded.getAddress());
        assertEquals(1, controller.listBeneficiaries().size());

        assertTrue(controller.deleteBeneficiary(saved.getId()).success());
        assertTrue(controller.listBeneficiaries().isEmpty());
    }

    @Test
    void nameIsRequiredAndContactDetailsAreCheckedWhenFilledIn() {
        assertEquals("Name is required", controller.saveBeneficiary(null, "", "", "", "", "").message());
        assertFalse(controller.saveBeneficiary(null, "Hoda", "bad-email", "", "", "").success());
        assertFalse(controller.saveBeneficiary(null, "Hoda", "", "abc", "", "").success());
        assertTrue(controller.listBeneficiaries().isEmpty());
    }
}
