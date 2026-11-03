package com.ripple.controller;

/**
 * The result of a user action, shown in the status bar (CR6).
 */
public record Result(boolean success, String message) {

    /** A successful result. */
    public static Result ok(String message) {
        return new Result(true, message);
    }

    /** A failed result. */
    public static Result fail(String message) {
        return new Result(false, message);
    }
}
