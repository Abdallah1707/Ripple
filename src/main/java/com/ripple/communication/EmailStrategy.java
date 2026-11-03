package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.service.InputValidator;

/**
 * Sends a message by email. Simulated, no real email is sent.
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
