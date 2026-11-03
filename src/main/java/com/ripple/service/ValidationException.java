package com.ripple.service;

/**
 * Thrown when input breaks a rule. The message is shown in the status bar.
 */
public class ValidationException extends RuntimeException {

    /** Creates the exception. */
    public ValidationException(String message) {
        super(message);
    }
}
