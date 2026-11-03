package com.ripple.communication;

import com.ripple.model.Contactable;
import com.ripple.repository.MessageLogRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends messages using the current strategy (Strategy pattern context).
 * The strategy can be changed with setStrategy. Every send, sent or skipped,
 * is saved in the message log.
 */
public class CommunicationService {

    private CommunicationStrategy strategy;
    private final MessageLogRepository messageLog;

    /** Creates the service with no strategy set. */
    public CommunicationService(MessageLogRepository messageLog) {
        this.messageLog = messageLog;
    }

    /** Sets how the next messages are sent. */
    public void setStrategy(CommunicationStrategy strategy) {
        this.strategy = strategy;
    }

    /** Returns the current strategy, or null if none was set. */
    public CommunicationStrategy getStrategy() {
        return strategy;
    }

    /**
     * Sends one message. People without a valid address are skipped.
     * Throws IllegalStateException if no strategy was set.
     */
    public DeliveryResult send(Contactable recipient, String subject, String body) {
        if (strategy == null) {
            throw new IllegalStateException("Choose Email, SMS or Social before sending");
        }
        String address = strategy.addressOf(recipient);
        Message message = new Message(recipient.getName(), address == null ? "" : address.trim(), subject, body);
        DeliveryResult result = strategy.isValidAddress(message.address())
                ? strategy.send(message)
                : DeliveryResult.failed("no valid " + strategy.getChannel().getAddressLabel());
        messageLog.save(strategy.getChannel(), message, result);
        return result;
    }

    /** Sends the same message to everyone in the list. */
    public List<DeliveryResult> sendToAll(List<? extends Contactable> recipients, String subject, String body) {
        List<DeliveryResult> results = new ArrayList<>();
        for (Contactable recipient : recipients) {
            results.add(send(recipient, subject, body));
        }
        return results;
    }
}
