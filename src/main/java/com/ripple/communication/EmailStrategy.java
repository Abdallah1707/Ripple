package com.ripple.communication;

import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.service.InputValidator;

/**
 * Concrete strategy in the Strategy pattern: sends a message by email, using the person's
 * email address.
 *
 * <p><b>Simulated:</b> no real email is sent; the message is printed to the console.
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
        System.out.println("[SIMULATED EMAIL] To: " + message.recipientName() + " <" + message.address() + ">"
                + " | Subject: " + message.subject() + " | " + message.body());
        return DeliveryResult.simulated("Email to " + message.address());
    }
}
