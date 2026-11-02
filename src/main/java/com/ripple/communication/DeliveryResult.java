package com.ripple.communication;

/**
 * What happened when Ripple tried to send one message.
 *
 * @param success {@code true} if the message was (simulated as) sent
 * @param status  {@code "SIMULATED"} or {@code "FAILED"}, saved in the message log
 * @param detail  a short explanation, e.g. why the message was skipped
 */
public record DeliveryResult(boolean success, String status, String detail) {

    /** Status of a message that was "sent" by a simulated strategy. */
    public static final String SIMULATED = "SIMULATED";
    /** Status of a message that was skipped or rejected. */
    public static final String FAILED = "FAILED";

    /**
     * Creates the result of a simulated send. Phase 1 never sends real email or SMS.
     *
     * @param detail what was done, e.g. "Email to mona@example.com"
     * @return a successful result with status {@code SIMULATED}
     */
    public static DeliveryResult simulated(String detail) {
        return new DeliveryResult(true, SIMULATED, detail);
    }

    /**
     * Creates the result of a message that could not be sent.
     *
     * @param detail why, written for the user, e.g. "no valid phone number"
     * @return a failed result with status {@code FAILED}
     */
    public static DeliveryResult failed(String detail) {
        return new DeliveryResult(false, FAILED, detail);
    }
}
