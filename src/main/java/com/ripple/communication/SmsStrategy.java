package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.service.InputValidator;

/**
 * Concrete strategy in the Strategy pattern: sends a message by SMS, using the person's
 * phone number. An SMS has no subject, and messages over {@value #MAX_LENGTH} characters
 * are rejected.
 *
 * <p><b>Simulated:</b> no real SMS is sent; the message is printed to the console.
 */
public class SmsStrategy implements CommunicationStrategy {

    /** The longest text a single SMS can carry. */
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
        System.out.println("[SIMULATED SMS] To: " + message.recipientName() + " <" + message.address() + ">"
                + " | " + message.body());
        return DeliveryResult.simulated("SMS to " + message.address());
    }
}
