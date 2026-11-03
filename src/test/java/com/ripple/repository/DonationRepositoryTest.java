package com.ripple.repository;

import com.ripple.TestDatabase;
import com.ripple.TestFactories;
import com.ripple.extras.DonationOptions;
import com.ripple.factory.DonationFactory;
import com.ripple.factory.DonationRequest;
import com.ripple.model.CashDonation;
import com.ripple.model.ChequeDonation;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.model.InKindDonation;
import com.ripple.model.OnlineDonation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationRepositoryTest {

    @TempDir
    Path tempDir;

    private Map<DonationMethod, DonationFactory> factories;
    private DonationRepository repository;
    private Donor donor;
    private DonationType type;

    @BeforeEach
    void setUp() {
        Database db = TestDatabase.create(tempDir);
        factories = TestFactories.all();
        repository = new DonationRepository(db, factories);
        donor = new DonorRepository(db).insert(new Donor(0, "Mona Ali", "mona@example.com", "", ContactChannel.EMAIL));
        type = new DonationTypeRepository(db).findAll().get(0);
    }

    private Donation save(DonationMethod method, DonationRequest request) {
        return repository.insert(TestFactories.create(method, donor, type, request));
    }

    @Test
    void decoratedDonationsAreSavedAsTheirPlainDonationAndComeBackDecorated() {
        Donation plain = TestFactories.create(DonationMethod.ONLINE, donor, type,
                new DonationRequest(250, "TX-9", null, 0, 0, null));
        Donation saved = repository.insert(new DonationOptions(true, "Hana Ali", true).decorate(plain));
        assertTrue(saved.getId() > 0);

        Donation loaded = repository.findAll().get(0);

        assertInstanceOf(OnlineDonation.class, loaded.getBase()); // rebuilt by its factory
        assertEquals("TX-9", ((OnlineDonation) loaded.getBase()).getTransactionReference());
        assertTrue(loaded.isUrgent());
        assertTrue(loaded.isAnonymous());
        assertEquals("Hana Ali", loaded.getHonoree());
        assertEquals(List.of("Gift", "Urgent", "Anonymous"), loaded.getOptions());
        assertEquals(saved.getReceiptNumber(), loaded.getReceiptNumber());
    }

    @Test
    void urgentDonationsAreListedFirst() {
        save(DonationMethod.CASH, new DonationRequest(100, null, null, 0, 0, null));
        Donation urgent = repository.insert(new DonationOptions(true, "", false).decorate(
                TestFactories.create(DonationMethod.CASH, donor, type, new DonationRequest(50, null, null, 0, 0, null))));
        save(DonationMethod.CASH, new DonationRequest(300, null, null, 0, 0, null)); // newest, but not urgent

        List<Donation> all = repository.findAll();

        assertEquals(urgent.getId(), all.get(0).getId());
        assertEquals(300, all.get(1).getValue()); // then newest
    }

    @Test
    void loadedDonationsAreRebuiltAsTheRightSubclass() {
        save(DonationMethod.CASH, new DonationRequest(100, null, null, 0, 0, null));
        save(DonationMethod.ONLINE, new DonationRequest(200, "TX-77", null, 0, 0, null));
        save(DonationMethod.IN_KIND, new DonationRequest(0, null, "Blankets", 4, 300, null));

        List<Donation> all = repository.findAll(); // newest first
        assertEquals(3, all.size());
        assertInstanceOf(InKindDonation.class, all.get(0));
        assertInstanceOf(OnlineDonation.class, all.get(1));
        assertInstanceOf(CashDonation.class, all.get(2));
        assertEquals("TX-77", ((OnlineDonation) all.get(1)).getTransactionReference());
        assertEquals(4, ((InKindDonation) all.get(0)).getQuantity());
    }

    @Test
    void chequeDonationIsSavedAndLoadedBackAsACheque() {
        save(DonationMethod.CHEQUE, new DonationRequest(2500, "000123", null, 0, 0, null));

        Donation loaded = repository.findAll().get(0);
        assertInstanceOf(ChequeDonation.class, loaded);
        assertEquals(DonationMethod.CHEQUE, loaded.getMethod());
        assertEquals("000123", ((ChequeDonation) loaded).getChequeNumber());
        assertEquals(2500.0, repository.totalValue(), 0.001); // cheque uses its amount
    }

    @Test
    void storedTimeAndRelatedObjectsAreKept() {
        LocalDateTime time = LocalDateTime.of(2026, 3, 5, 10, 30);
        Donation saved = save(DonationMethod.CASH, new DonationRequest(100, null, null, 0, 0, time));

        Donation loaded = repository.findAll().get(0);
        assertEquals(time, loaded.getCreatedAt());
        assertEquals(saved.getId(), loaded.getId());
        assertEquals(saved.getReceiptNumber(), loaded.getReceiptNumber());
        assertEquals("Mona Ali", loaded.getDonor().getName());
        assertEquals(type.getName(), loaded.getDonationType().getName());
    }

    @Test
    void countAndTotalValueUseAmountOrEstimatedValue() {
        assertEquals(0, repository.count());
        assertEquals(0.0, repository.totalValue());

        save(DonationMethod.CASH, new DonationRequest(100, null, null, 0, 0, null));
        save(DonationMethod.IN_KIND, new DonationRequest(0, null, "Books", 2, 50, null));

        assertEquals(2, repository.count());
        assertEquals(150.0, repository.totalValue(), 0.001);
        assertTrue(repository.findAll().get(0).getId() > repository.findAll().get(1).getId());
    }
}
