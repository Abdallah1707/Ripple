package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.service.InputValidator;

/**
 * Concrete strategy in the Strategy pattern: sends a message by email, using the person's
 * email address.
 *
 * <p><b>Simulated:</b> no real email is sent. The result is status {@code SIMULATED},
 * and {@code CommunicationService} records the message in the message log shown on screen.
 */
public class EmailStrategy implements CommunicationStrategy {

    @Override
    public ContactChannel getChannel() {
        return ContactChannel.EMAIL;
    }

    @Override
    public String addressOf(Contactable recipient) {
        return recipient.getEmail();
    }

    @Override
    public boolean isValidAddress(String address) {
        return InputValidator.isValidEmail(address);
    }

    @Override
    public DeliveryResult send(Message message) {
        return DeliveryResult.simulated("Email to " + message.address());
    }
}
