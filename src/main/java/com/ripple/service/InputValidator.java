package com.ripple.service;

import java.util.regex.Pattern;

/**
 * Small shared input checks, so every service and controller validates text, emails,
 * phones and numbers the same way. All methods are static; this class holds no state.
 */
public final class InputValidator {

    /** Something@something.something with no spaces. Deliberately simple. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    /** Optional leading +, then 8 to 15 digits (after spaces and dashes are removed). */
    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{8,15}$");

    private InputValidator() {
    }

    /**
     * Checks that a required text field is filled in and not too short or long.
     *
     * @param value the user's input (may be {@code null})
     * @param field the field's name for the error message, e.g. {@code "Name"}
     * @param min   minimum length after trimming
     * @param max   maximum length after trimming
     * @return the trimmed value
     * @throws ValidationException if the value is blank or its length is out of range
     */
    public static String requireText(String value, String field, int min, int max) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(field + " is required");
        }
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new ValidationException(field + " must be between " + min + " and " + max + " characters");
        }
        return trimmed;
    }

    /**
     * Tells whether the text looks like an email address.
     *
     * @param value the text to check (may be {@code null})
     * @return {@code true} for e.g. {@code "mona@example.com"}
     */
    public static boolean isValidEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    /**
     * Tells whether the text looks like a phone number. Spaces and dashes are ignored.
     *
     * @param value the text to check (may be {@code null})
     * @return {@code true} for e.g. {@code "010 1234 5678"} or {@code "+201012345678"}
     */
    public static boolean isValidPhone(String value) {
        return value != null && PHONE.matcher(value.replaceAll("[\\s-]", "")).matches();
    }

    /**
     * Converts the user's input to a decimal number.
     *
     * @param value the user's input
     * @param field the field's name for the error message
     * @return the number
     * @throws ValidationException if the input is blank or not a number
     */
    public static double parseDouble(String value, String field) {
        String trimmed = requireText(value, field, 1, 30);
        try {
            double number = Double.parseDouble(trimmed);
            if (Double.isFinite(number)) { // rejects "NaN" and "Infinity", which Java would accept
                return number;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        throw new ValidationException(field + " must be a number");
    }

    /**
     * Converts the user's input to a whole number.
     *
     * @param value the user's input
     * @param field the field's name for the error message
     * @return the number
     * @throws ValidationException if the input is blank or not a whole number
     */
    public static int parseInt(String value, String field) {
        String trimmed = requireText(value, field, 1, 10);
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a whole number");
        }
    }
}
