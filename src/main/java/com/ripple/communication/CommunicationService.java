package com.ripple.communication;

import com.ripple.model.Contactable;
import com.ripple.repository.MessageLogRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Context in the Strategy pattern: sends messages with whichever
 * {@link CommunicationStrategy} is currently set.
 *
 * <p>The strategy can be swapped at any time with {@link #setStrategy(CommunicationStrategy)}.
 * This class never checks which channel it is using (no "if email ... else if SMS ..."):
 * it asks the current strategy for the address, checks it, and sends. Every attempt, sent
 * or skipped, is saved in the message log.
 *
 * <p>Clients ({@code CommunicationController} and {@code DonorNotificationObserver}) share
 * one instance, so each of them sets the strategy right before it sends.
 */
public class CommunicationService {

    private CommunicationStrategy strategy;
    private final MessageLogRepository messageLog;

    /**
     * Creates the service with no strategy chosen yet.
     *
     * @param messageLog where every send attempt is saved
     */
    public CommunicationService(MessageLogRepository messageLog) {
        this.messageLog = messageLog;
    }

    /**
     * Chooses how the next messages are sent.
     *
     * @param strategy e.g. an {@link EmailStrategy} or an {@link SmsStrategy}
     */
    public void setStrategy(CommunicationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns the current strategy.
     *
     * @return the strategy, or {@code null} if none was set yet
     */
    public CommunicationStrategy getStrategy() {
        return strategy;
    }

    /**
     * Sends one message with the current strategy. A person without a valid address for
     * this channel is skipped. Either way the attempt is saved in the message log.
     *
     * @param recipient the person to send to
     * @param subject   the subject line
     * @param body      the message text
     * @return what happened
     * @throws IllegalStateException if no strategy was set
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

    /**
     * Sends the same message to everyone in a list with the current strategy.
     *
     * @param recipients the people to send to
     * @param subject    the subject line
     * @param body       the message text
     * @return one result per recipient, in the same order
     */
    public List<DeliveryResult> sendToAll(List<? extends Contactable> recipients, String subject, String body) {
        List<DeliveryResult> results = new ArrayList<>();
        for (Contactable recipient : recipients) {
            results.add(send(recipient, subject, body));
        }
        return results;
    }
}
