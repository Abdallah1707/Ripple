package com.ripple.service;

import java.util.regex.Pattern;

/**
 * Shared input checks for text, email, phone and numbers.
 */
public final class InputValidator {

    /** Simple email check. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    /** Optional +, then 8 to 15 digits. */
    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{8,15}$");
    /** @ then 2 to 30 letters, digits, _ or dots. */
    private static final Pattern SOCIAL_HANDLE = Pattern.compile("^@[A-Za-z0-9_.]{2,30}$");

    private InputValidator() {
    }

    /** Checks a required text field and its length. Returns it trimmed. */
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

    /** True if it looks like an email. */
    public static boolean isValidEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    /** True if it looks like a phone number. Spaces and dashes are ignored. */
    public static boolean isValidPhone(String value) {
        return value != null && PHONE.matcher(value.replaceAll("[\\s-]", "")).matches();
    }

    /** True if it looks like a handle, like @mona_ali. */
    public static boolean isValidSocialHandle(String value) {
        return value != null && SOCIAL_HANDLE.matcher(value.trim()).matches();
    }

    /** Email that can be empty, but must be valid if typed. */
    public static String optionalEmail(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.isEmpty() && !isValidEmail(trimmed)) {
            throw new ValidationException("Email is not valid (example: name@example.com)");
        }
        return trimmed;
    }

    /** Phone that can be empty, but must be valid if typed. */
    public static String optionalPhone(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.isEmpty() && !isValidPhone(trimmed)) {
            throw new ValidationException("Phone number is not valid (8 to 15 digits)");
        }
        return trimmed;
    }

    /** Parses a decimal number. */
    public static double parseDouble(String value, String field) {
        String trimmed = requireText(value, field, 1, 30);
        try {
            double number = Double.parseDouble(trimmed);
            if (Double.isFinite(number)) { // rejects NaN and Infinity
                return number;
            }
        } catch (NumberFormatException ignored) {
            // goes to the error below
        }
        throw new ValidationException(field + " must be a number");
    }

    /** Parses a whole number. */
    public static int parseInt(String value, String field) {
        String trimmed = requireText(value, field, 1, 10);
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a whole number");
        }
    }
}
