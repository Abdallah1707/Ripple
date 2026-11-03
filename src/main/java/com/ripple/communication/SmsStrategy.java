package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.service.InputValidator;

/**
 * Sends a message by SMS. No subject, and max 160 characters.
 * Simulated, no real SMS is sent.
 */
public class SmsStrategy implements CommunicationStrategy {

    /** Max SMS length. */
    public static final int MAX_LENGTH = 160;

    @Override
    public ContactChannel getChannel() {
        return ContactChannel.SMS;
    }

    @Override
    public String addressOf(Contactable recipient) {
        return recipient.getPhone();
    }

    @Override
    public boolean isValidAddress(String address) {
        return InputValidator.isValidPhone(address);
    }

    @Override
    public DeliveryResult send(Message message) {
        if (message.body().length() > MAX_LENGTH) {
            return DeliveryResult.failed("message is longer than " + MAX_LENGTH + " characters");
        }
        return DeliveryResult.simulated("SMS to " + message.address());
    }
}
