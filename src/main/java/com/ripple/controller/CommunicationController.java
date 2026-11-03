package com.ripple.controller;

import com.ripple.communication.CommunicationService;
import com.ripple.communication.CommunicationStrategy;
import com.ripple.communication.DeliveryResult;
import com.ripple.communication.MessageLogEntry;
import com.ripple.communication.SmsStrategy;
import com.ripple.communication.SocialMediaStrategy;
import com.ripple.logging.ApplicationLogger;
import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.repository.BeneficiaryRepository;
import com.ripple.repository.DonorRepository;
import com.ripple.repository.EventRepository;
import com.ripple.repository.MessageLogRepository;
import com.ripple.repository.VolunteerRepository;
import com.ripple.service.InputValidator;
import com.ripple.service.ValidationException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for the Communication screen.
 * Picks the strategy for the chosen channel and sends to the chosen group.
 */
public class CommunicationController {

    private final CommunicationService communicationService;
    private final Map<ContactChannel, CommunicationStrategy> strategies;
    private final DonorRepository donorRepository;
    private final VolunteerRepository volunteerRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final EventRepository eventRepository;
    private final MessageLogRepository messageLogRepository;

    /** Creates the controller. */
    public CommunicationController(CommunicationService communicationService,
                                   Map<ContactChannel, CommunicationStrategy> strategies,
                                   DonorRepository donorRepository, VolunteerRepository volunteerRepository,
                                   BeneficiaryRepository beneficiaryRepository, EventRepository eventRepository,
                                   MessageLogRepository messageLogRepository) {
        this.communicationService = communicationService;
        this.strategies = strategies;
        this.donorRepository = donorRepository;
        this.volunteerRepository = volunteerRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.eventRepository = eventRepository;
        this.messageLogRepository = messageLogRepository;
    }

    /** Sends a message to a whole group on one channel. */
    public Result sendToGroup(Audience audience, Integer eventId, String channel, String subject, String body) {
        try {
            if (audience == null) {
                throw new ValidationException("Choose who to send the message to");
            }
            CommunicationStrategy strategy = strategies.get(parseChannel(channel));
            String cleanSubject = InputValidator.requireText(subject, "Subject", 1, 100);
            String cleanBody = InputValidator.requireText(body, "Message", 1, 1000);
            List<? extends Contactable> recipients = loadRecipients(audience, eventId);
            if (recipients.isEmpty()) {
                return Result.fail("There is nobody to send to in '" + audience.getLabel() + "'");
            }

            communicationService.setStrategy(strategy);
            List<DeliveryResult> results = communicationService.sendToAll(recipients, cleanSubject, cleanBody);

            String summary = summarize(results);
            ApplicationLogger.getInstance().info(strategy.getChannel() + " message '" + cleanSubject + "' to "
                    + audience.getLabel() + ": " + summary);
            boolean anySent = results.stream().anyMatch(DeliveryResult::success);
            return anySent ? Result.ok(summary) : Result.fail(summary);
        } catch (ValidationException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalStateException e) {
            ApplicationLogger.getInstance().error(e.getMessage());
            return Result.fail("Could not send the message because of a database error. Details are in logs/ripple.log.");
        }
    }

    /** The message log, newest first. */
    public List<MessageLogEntry> listMessageLog() {
        return messageLogRepository.findAll();
    }

    /** Max message length for a channel, or 0 if there is no limit. */
    public int getMessageLimit(ContactChannel channel) {
        if (channel == ContactChannel.SMS) {
            return SmsStrategy.MAX_LENGTH;
        }
        if (channel == ContactChannel.SOCIAL) {
            return SocialMediaStrategy.MAX_LENGTH;
        }
        return 0;
    }

    /** Loads the people in the chosen group. */
    private List<? extends Contactable> loadRecipients(Audience audience, Integer eventId) {
        return switch (audience) {
            case DONORS -> donorRepository.findAll();
            case VOLUNTEERS -> volunteerRepository.findAll();
            case BENEFICIARIES -> beneficiaryRepository.findAll();
            case EVENT_ATTENDEES -> {
                if (eventId == null || eventRepository.findById(eventId).isEmpty()) {
                    throw new ValidationException("Choose an event to message its attendees");
                }
                yield eventRepository.findAttendees(eventId);
            }
        };
    }

    /** Builds the "Sent X, skipped Y" message. */
    private static String summarize(List<DeliveryResult> results) {
        long sent = results.stream().filter(DeliveryResult::success).count();
        Set<String> reasons = new LinkedHashSet<>();
        for (DeliveryResult result : results) {
            if (!result.success()) {
                reasons.add(result.detail());
            }
        }
        long skipped = results.size() - sent;
        String summary = "Sent " + sent + ", skipped " + skipped;
        return reasons.isEmpty() ? summary : summary + " (" + String.join("; ", reasons) + ")";
    }

    /** Turns "EMAIL" / "SMS" / "SOCIAL" into a channel. */
    private ContactChannel parseChannel(String channel) {
        if (channel != null) {
            for (ContactChannel value : ContactChannel.values()) {
                if (value.name().equalsIgnoreCase(channel.trim()) && strategies.containsKey(value)) {
                    return value;
                }
            }
        }
        throw new ValidationException("Channel must be Email, SMS or Social");
    }
}
