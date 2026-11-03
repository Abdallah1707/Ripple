# Ripple — CRUD Matrix: Donations module

> Which **function** of the Donations module **C**reates, **R**eads, **U**pdates or **D**eletes which **entity**.
> This is the one complete module required for Phase 1. The other modules use the same layers (see [architecture.md](architecture.md)).

Related: [class-diagram.md](class-diagram.md) (classes and patterns) · [README](../README.md) (requirements)

---

## 1. The matrix

Rows are the functions a staff member can use on the **Donations** screen (two tabs: *Donations* and *Donors*). Columns are the entities (database tables) the module touches.

| # | Function (user requirement) | Donor | Donation | DonationType | Receipt | MessageLog |
|---|---|:---:|:---:|:---:|:---:|:---:|
| F1 | Register a donor (*Donors* tab → Add) | **C** | | | | |
| F2 | View the list of donors (*Donors* table, donor drop-down) | **R** | | | | |
| F3 | Edit a donor (*Donors* tab → Update) | **R U** | | | | |
| F4 | Delete a donor (*Donors* tab → Delete) | **R D** | **R** | | | |
| F5 | Choose a donation type for a donation | | | **R** | | |
| F6 | Record a donation (cash, online, in-kind or cheque) | **R** | **C** | **R** | **C** | **C** |
| F7 | View all donations (*Donations* table) | **R** | **R** | **R** | | |
| F8 | View a donation's receipt (*View receipt*) | | | | **R** | |
| F9 | Create a donor's yearly tax statement (*Donors* tab → Tax statement) | **R** | **R** | **R** | | |
| **Total** | | **C R U D** | **C R** | **R** | **C R** | **C** |

Notes on the cells:

- **F3, F4 — Donor R:** the donor is read first, so editing or deleting an id that no longer exists gives a clear message instead of doing nothing.
- **F4 — Donation R:** before deleting, the donor's donations are counted. A donor with donations **cannot be deleted** ("'Mona Ali' has 2 donation(s) and cannot be deleted"), so receipts and the donation history stay complete. This is the same rule as CR2 for lookup values that are in use.
- **F6 — Receipt C and MessageLog C** are done by **observers**, not by the donation code itself (Observer pattern): `ReceiptObserver` saves the decorated receipt, and `DonorNotificationObserver` sends the thank-you and logs it. MessageLog **C** only happens while "Notify donors on new donation" is ticked. The new donation itself is created by the creator for its method (Factory Method pattern).
- **F7 — Donor R, DonationType R:** each donation row is loaded together with its donor and type (one SQL join).
- **F9 — Donor R, Donation R, DonationType R:** the tax statement is built when it is asked for and is not saved. It reads the donor, their donations of the chosen year, and each donation's type, and keeps only the donations whose type is tax-deductible.

## 2. Why some cells are empty

| Entity | Missing | Why |
|---|---|---|
| Donation | **U**, **D** | A recorded donation is a financial record with a receipt number that has already been issued to the donor (CR3). Changing or deleting it would make the receipt wrong, so a mistake is corrected by recording a new donation, the same way accounting systems work. |
| DonationType | **C**, **U**, **D** | Donation types are a lookup table, managed in the **Lookup Management** module (its own CRUD, with CR1 and CR2). The Donations module only reads them. |
| Receipt | **U**, **D** | A receipt is created once, when the donation is recorded, and then only read. |
| MessageLog | **R**, **U**, **D** | The message log is read on the **Communication** screen, not in this module, and log entries are never changed. |

## 3. Where each letter happens in the code

Every function goes through the same MVC path: **View → Controller → Service → Repository**.

| # | View (`DonationsPanel`) | Controller (`DonationController`) | Service (`DonationService`) | Repository (SQL) |
|---|---|---|---|---|
| F1 | *Add* button | `saveDonor(null, ...)` | `registerDonor(...)` | `DonorRepository.insert` → `INSERT INTO donors` |
| F2 | `refresh()` | `listDonors()` | `findAllDonors()` | `DonorRepository.findAll` → `SELECT ... FROM donors` |
| F3 | *Update* button | `saveDonor(id, ...)` | `updateDonor(...)` | `DonorRepository.findById`, `update` → `UPDATE donors` |
| F4 | *Delete* button (asks "Delete donor ...?") | `deleteDonor(id)` | `deleteDonor(id)` | `DonorRepository.findById`, `countDonations` → `SELECT COUNT(*) FROM donations`, `delete` → `DELETE FROM donors` |
| F5 | `refresh()` | `LookupController.listDonationTypes()` | `LookupService.findAll()` | `DonationTypeRepository.findAll` |
| F6 | *Record donation* button | `recordDonation(...)` | `recordDonation(...)` → creator `orderDonation(...)` → `notifyObservers(...)` | `DonorRepository.findById`, `DonationTypeRepository.findById`, `DonationRepository.insert`; observers: `ReceiptRepository.save`, `MessageLogRepository.save` |
| F7 | `refresh()` | `listDonations()` | `findAllDonations()` | `DonationRepository.findAll` → `SELECT ... JOIN donors JOIN donation_types` |
| F8 | *View receipt* button | `getReceipt(id)` | — | `ReceiptRepository.findByDonationId` → `SELECT content FROM receipts` |
| F9 | *Tax statement* button (asks for the year) | `getTaxStatement(donorId, year)` | `buildTaxStatement(donorId, year)` | `DonorRepository.findById`, `DonationRepository.findAll`; the service keeps the donor's tax-deductible donations of that year |

## 4. Tests that cover the matrix

| Letters | Test |
|---|---|
| Donor **C R U D**, delete blocked while in use | `DonorRepositoryTest`, `DonationServiceTest` (update / delete section), `DonationControllerTest` (`saveDonor...`, `deleteDonor...`) |
| Donation **C R** for all four methods | `DonationServiceTest`, `DonationRepositoryTest` (including a cheque saved and loaded back), `DonationControllerTest` |
| Receipt **C R** | `ReceiptObserverTest`, `ReceiptRepositoryTest`, `DonationControllerTest` (receipt with and without tax information) |
| MessageLog **C** | `DonorNotificationObserverTest` |
| F9 tax statement (Donor, Donation, DonationType **R**) | `TaxStatementTest`, `DonationServiceTest` (tax statements section), `DonationControllerTest` |
