package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;

/**
 * A way of sending a message (email, SMS, social).
 * Adding a new channel only needs a new class that implements this.
 */
public interface CommunicationStrategy {

    /** The channel this strategy uses. */
    ContactChannel getChannel();

    /** Gets the address this channel uses for a person (can be empty). */
    String addressOf(Contactable recipient);

    /** Checks if the address can be used on this channel. */
    boolean isValidAddress(String address);

    /** Sends a message. In Phase 1 this is only simulated. */
    DeliveryResult send(Message message);
}
