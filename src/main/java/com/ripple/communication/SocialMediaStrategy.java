package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.service.InputValidator;

/**
 * Concrete strategy in the Strategy pattern: sends a message as a social media direct
 * message, using the person's handle (e.g. {@code @mona_ali}). Like a short post, it has no
 * subject, and messages over {@value #MAX_LENGTH} characters are rejected.
 *
 * <p>Only donors have a handle, so sending to anyone else fails with "no valid social media
 * handle" in the message log, just as SMS fails for someone without a phone number.
 *
 * <p><b>Simulated:</b> nothing is posted to a real platform. The result is status
 * {@code SIMULATED}, and {@code CommunicationService} records the message in the message log.
 */
public class SocialMediaStrategy implements CommunicationStrategy {

    /** The longest text a single social media message can carry. */
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
