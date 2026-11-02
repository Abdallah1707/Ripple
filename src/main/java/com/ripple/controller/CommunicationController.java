package com.ripple.controller;

import com.ripple.communication.CommunicationService;
import com.ripple.communication.CommunicationStrategy;
import com.ripple.communication.DeliveryResult;
import com.ripple.communication.MessageLogEntry;
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
 * Controller for the Communication screen, and a client of the Strategy pattern.
 *
 * <p>It loads the chosen group of people, picks the strategy for the chosen channel from
 * its map (no "if email ... else if SMS ..."), gives it to {@link CommunicationService} with
 * {@code setStrategy(...)}, sends, and sums up the outcome for the status bar, e.g.
 * <i>"Sent 4, skipped 2 (no valid phone number)"</i>. It never throws to the view.
 */
public class CommunicationController {

    private final CommunicationService communicationService;
    private final Map<ContactChannel, CommunicationStrategy> strategies;
    private final DonorRepository donorRepository;
    private final VolunteerRepository volunteerRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final EventRepository eventRepository;
    private final MessageLogRepository messageLogRepository;

    /**
     * Creates the controller.
     *
     * @param communicationService  the Strategy context that sends the messages
     * @param strategies            one strategy per channel
     * @param donorRepository       where donors are read from
     * @param volunteerRepository   where volunteers are read from
     * @param beneficiaryRepository where beneficiaries are read from
     * @param eventRepository       where an event's attendees are read from
     * @param messageLogRepository  where the message log is read from
     */
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

    /**
     * Sends a message to a whole group on one channel.
     *
     * @param audience who to send to
     * @param eventId  the chosen event, only used for {@link Audience#EVENT_ATTENDEES}
     * @param channel  {@code "EMAIL"} or {@code "SMS"}
     * @param subject  the subject typed by the user
     * @param body     the message typed by the user
     * @return how many were sent and skipped, for the status bar
     */
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

    /**
     * Returns the message log, newest first, for the table.
     *
     * @return all log entries
     */
    public List<MessageLogEntry> listMessageLog() {
        return messageLogRepository.findAll();
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

    /** Builds e.g. "Sent 4, skipped 2 (no valid phone number)". */
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

    /** Converts {@code "EMAIL"} / {@code "SMS"} to a channel, or explains what is wrong. */
    private ContactChannel parseChannel(String channel) {
        if (channel != null) {
            for (ContactChannel value : ContactChannel.values()) {
                if (value.name().equalsIgnoreCase(channel.trim()) && strategies.containsKey(value)) {
                    return value;
                }
            }
        }
        throw new ValidationException("Channel must be Email or SMS");
    }
}
