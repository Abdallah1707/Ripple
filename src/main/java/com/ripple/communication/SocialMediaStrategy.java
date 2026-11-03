package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.service.InputValidator;

/**
 * Sends a message as a social media DM using the donor's handle.
 * No subject, and max 280 characters. Only donors have a handle.
 * Simulated, nothing is really posted.
 */
public class SocialMediaStrategy implements CommunicationStrategy {

    /** Max message length. */
    public static final int MAX_LENGTH = 280;

    @Override
    public ContactChannel getChannel() {
        return ContactChannel.SOCIAL;
    }

    @Override
    public String addressOf(Contactable recipient) {
        return recipient.getSocialHandle();
    }

    @Override
    public boolean isValidAddress(String address) {
        return InputValidator.isValidSocialHandle(address);
    }

    @Override
    public DeliveryResult send(Message message) {
        if (message.body().length() > MAX_LENGTH) {
            return DeliveryResult.failed("message is longer than " + MAX_LENGTH + " characters");
        }
        return DeliveryResult.simulated("Social media message to " + message.address());
    }
}
