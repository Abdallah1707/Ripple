package com.ripple.controller;

/**
 * Outcome of a user action, returned by every controller method that changes data.
 *
 * <p>Views never decide whether an action worked; they show the {@code Result}
 * the controller gives them in the status bar (client requirement CR6).
 *
 * @param success {@code true} if the action worked
 * @param message text to show to the user
 */
public record Result(boolean success, String message) {

    /**
     * Creates a successful result.
     *
     * @param message what was done, e.g. "Donation type 'Zakat' added"
     * @return a result with {@code success == true}
     */
    public static Result ok(String message) {
        return new Result(true, message);
    }

    /**
     * Creates a failed result.
     *
     * @param message why it failed, written for the user
     * @return a result with {@code success == false}
     */
    public static Result fail(String message) {
        return new Result(false, message);
    }
}
