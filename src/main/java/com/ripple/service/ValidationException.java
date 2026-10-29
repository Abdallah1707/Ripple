package com.ripple.service;

/**
 * Thrown when user input breaks a business rule. The message is written for the user
 * and is shown as-is (in red) in the status bar.
 */
public class ValidationException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what is wrong, in plain English, e.g. "Name is required"
     */
    public ValidationException(String message) {
        super(message);
    }
}
