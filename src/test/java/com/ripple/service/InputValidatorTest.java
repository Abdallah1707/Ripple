package com.ripple.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

    @Test
    void requireTextTrimsAndChecksLength() {
        assertEquals("Zakat", InputValidator.requireText("  Zakat ", "Name", 2, 50));
        assertEquals("Name is required",
                assertThrows(ValidationException.class, () -> InputValidator.requireText("   ", "Name", 2, 50)).getMessage());
        assertEquals("Name must be between 2 and 50 characters",
                assertThrows(ValidationException.class, () -> InputValidator.requireText("Z", "Name", 2, 50)).getMessage());
    }

    @Test
    void emailAndPhoneChecks() {
        assertTrue(InputValidator.isValidEmail("mona@example.com"));
        assertFalse(InputValidator.isValidEmail("mona@example"));
        assertFalse(InputValidator.isValidEmail(null));
        assertTrue(InputValidator.isValidPhone("010 1234-5678"));
        assertTrue(InputValidator.isValidPhone("+201012345678"));
        assertFalse(InputValidator.isValidPhone("12345"));
        assertFalse(InputValidator.isValidPhone("01012abc678"));
    }

    @Test
    void numberParsing() {
        assertEquals(12.5, InputValidator.parseDouble(" 12.5 ", "Amount"));
        assertEquals("Amount must be a number",
                assertThrows(ValidationException.class, () -> InputValidator.parseDouble("abc", "Amount")).getMessage());
        assertThrows(ValidationException.class, () -> InputValidator.parseDouble("NaN", "Amount"));
        assertEquals("Amount is required",
                assertThrows(ValidationException.class, () -> InputValidator.parseDouble("", "Amount")).getMessage());
        assertEquals(3, InputValidator.parseInt("3", "Quantity"));
        assertEquals("Quantity must be a whole number",
                assertThrows(ValidationException.class, () -> InputValidator.parseInt("2.5", "Quantity")).getMessage());
    }
}
