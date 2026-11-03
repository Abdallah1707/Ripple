package com.ripple.communication;

/**
 * The result of trying to send one message.
 */
public record DeliveryResult(boolean success, String status, String detail) {

    /** Status when a message was sent (simulated). */
    public static final String SIMULATED = "SIMULATED";
    /** Status when a message was skipped or rejected. */
    public static final String FAILED = "FAILED";

    /** A successful simulated send. */
    public static DeliveryResult simulated(String detail) {
        return new DeliveryResult(true, SIMULATED, detail);
    }

    /** A message that could not be sent. */
    public static DeliveryResult failed(String detail) {
        return new DeliveryResult(false, FAILED, detail);
    }
}
