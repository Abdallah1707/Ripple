package com.ripple.communication;

import com.ripple.TestDatabase;
import com.ripple.model.Beneficiary;
import com.ripple.model.ContactChannel;
import com.ripple.model.Contactable;
import com.ripple.model.Donor;
import com.ripple.repository.MessageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the communication strategies and the message log. */
class CommunicationServiceTest {

    @TempDir
    Path tempDir;

    private MessageLogRepository log;
    private CommunicationService service;
    private final Contactable mona = new Beneficiary(1, "Mona Ali", "mona@example.com", "01012345678", "", "");

    @BeforeEach
    void setUp() {
        log = new MessageLogRepository(TestDatabase.create(tempDir));
        service = new CommunicationService(log);
    }

    @Test
    void switchingFromEmailToSmsChangesWhichAddressIsUsed() {
        service.setStrategy(new EmailStrategy());
        assertTrue(service.send(mona, "Hello", "Hi Mona").success());
        service.setStrategy(new SmsStrategy());
        assertTrue(service.send(mona, "Hello", "Hi Mona").success());

        List<MessageLogEntry> entries = log.findAll(); // newest first
        assertEquals(2, entries.size());
        assertEquals(ContactChannel.SMS, entries.get(0).channel());
        assertTrue(entries.get(0).recipient().contains("01012345678"));
        assertEquals(ContactChannel.EMAIL, entries.get(1).channel());
        assertTrue(entries.get(1).recipient().contains("mona@example.com"));
        assertEquals(DeliveryResult.SIMULATED, entries.get(0).status());
    }

    @Test
    void peopleWithoutAValidAddressAreSkippedAndLogged() {
        Contactable noPhone = new Beneficiary(2, "Omar", "omar@example.com", "", "", "");
        service.setStrategy(new SmsStrategy());

        List<DeliveryResult> results = service.sendToAll(List.of(mona, noPhone), "Hello", "Hi");

        assertTrue(results.get(0).success());
        assertFalse(results.get(1).success());
        assertEquals("no valid phone number", results.get(1).detail());
        List<MessageLogEntry> entries = log.findAll();
        assertEquals(2, entries.size());
        assertEquals("FAILED: no valid phone number", entries.get(0).status());
        assertEquals("Omar", entries.get(0).recipient());
    }

    @Test
    void smsRejectsMessagesOver160CharactersButEmailDoesNot() {
        String longText = "x".repeat(SmsStrategy.MAX_LENGTH + 1);

        service.setStrategy(new SmsStrategy());
        DeliveryResult sms = service.send(mona, "Hello", longText);
        service.setStrategy(new EmailStrategy());
        DeliveryResult email = service.send(mona, "Hello", longText);

        assertFalse(sms.success());
        assertTrue(sms.detail().contains("160"));
        assertTrue(email.success());
        assertTrue(new SmsStrategy().send(new Message("Mona", "01012345678", "", "x".repeat(160))).success());
    }

    @Test
    void socialMediaReachesDonorsWithAHandleAndSkipsEveryoneElse() {
        Contactable donor = new Donor(3, "Sara", "", "", "@sara_s", ContactChannel.SOCIAL);
        service.setStrategy(new SocialMediaStrategy());

        List<DeliveryResult> results = service.sendToAll(List.of(donor, mona), "Hello", "Thank you!");

        assertTrue(results.get(0).success());
        assertFalse(results.get(1).success()); // beneficiaries have no handle
        assertEquals("no valid social media handle", results.get(1).detail());
        List<MessageLogEntry> entries = log.findAll(); // newest first
        assertEquals(ContactChannel.SOCIAL, entries.get(1).channel());
        assertTrue(entries.get(1).recipient().contains("@sara_s"));
        assertEquals(DeliveryResult.SIMULATED, entries.get(1).status());
    }

    @Test
    void socialMediaRejectsMessagesOver280Characters() {
        SocialMediaStrategy social = new SocialMediaStrategy();
        assertTrue(social.send(new Message("Sara", "@sara_s", "", "x".repeat(280))).success());
        DeliveryResult tooLong = social.send(new Message("Sara", "@sara_s", "", "x".repeat(281)));
        assertFalse(tooLong.success());
        assertTrue(tooLong.detail().contains("280"));
    }

    @Test
    void sendingWithoutAStrategyIsNotAllowed() {
        assertThrows(IllegalStateException.class, () -> service.send(mona, "Hello", "Hi"));
        assertTrue(log.findAll().isEmpty());
    }
}
