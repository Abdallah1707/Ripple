package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;

/**
 * Strategy interface in the Strategy pattern: one way of sending a message (Email, SMS, ...).
 *
 * <p>{@link CommunicationService} (the context) only talks to this interface, so a channel
 * can be swapped while the app runs with {@code setStrategy(...)}, and a new channel is just
 * one new class that implements it.
 */
public interface CommunicationStrategy {

    /**
     * Returns the channel this strategy sends on.
     *
     * @return e.g. {@link ContactChannel#EMAIL}
     */
    ContactChannel getChannel();

    /**
     * Picks the address this channel uses for a person.
     *
     * @param recipient the person to contact
     * @return their email address or phone number (may be empty)
     */
    String addressOf(Contactable recipient);

    /**
     * Tells whether an address can be used on this channel.
     *
     * @param address the address returned by {@link #addressOf(Contactable)}
     * @return {@code true} if a message can be sent to it
     */
    boolean isValidAddress(String address);

    /**
     * Sends one message. In Phase 1 this is simulated: nothing leaves the computer and the
     * result is status {@code SIMULATED}; the message log is the visible output.
     *
     * @param message the message, already addressed
     * @return what happened
     */
    DeliveryResult send(Message message);
}
