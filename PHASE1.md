# Ripple — Phase 1 Plan

Ripple is a desktop app (Java + Swing) that helps a charity manage its **donations, volunteers, events, beneficiaries and messages**. This file explains what is already built, how the remaining work is split into **6 sections**, and what each section needs to do.

> **How to use this file:** find your section, follow its steps from top to bottom, tick the boxes as you go, and update the progress table at the end when you finish.

---

## Progress

| Section | What it builds | Owner | Status |
|:-:|---|---|:-:|
| 0 | Skeleton + design documents | — | Done |
| 1 | Foundation, lookup tables, Singleton, Factory | Abdallah Momen | Done |
| 2 | Donations + Observer |Sandra |IN Prograss |
| 3 | Receipts + Decorator | | Not started |
| 4 | Volunteers, Events, Beneficiaries | | Not started |
| 5 | Messages + Strategy | | Not started |
| 6 | Dashboard + final polish | | Not started |

Status values: Not started, In progress, Done.

---

## How Phase 1 is graded (10 marks)

| Item | Marks | Section |
|---|:-:|:-:|
| Class diagram | 2 | everyone, finished in 6 |
| MVC structure | 2 | everyone, checked in 6 |
| User control panel (the main window) | 1 | everyone, finished in 6 |
| Strategy pattern | 1 | 5 |
| Observer pattern | 1 | 2 |
| Decorator pattern | 1 | 3 |
| Factory pattern | 0.5 | 1 |
| Singleton pattern | 0.5 | 1 |
| Lookup tables (add / edit / delete) | 0.5 | 1 |
| Extra client requirements | 0.5 | see below |

---

## What is already done

- **The main window.** Run the app and you get a sidebar with 7 buttons (Dashboard, Donations, Volunteers, Events, Beneficiaries, Communication, Lookup Management). Each screen is currently a placeholder that says which section builds it.
- **The status bar** at the bottom, which shows green messages for success and red for errors.
- **The folder structure.** One folder (package) per layer or pattern, each with a short note explaining what goes in it.
- **The class diagram** in [docs/class-diagram.md](docs/class-diagram.md). It lists every class we will write, with its fields and methods.
- **The architecture notes** in [docs/architecture.md](docs/architecture.md). These explain how the pieces talk to each other and what the database tables look like.

### Run it

You need **Java 21** and **Maven** installed.

```bash
mvn compile exec:java
```

Other useful commands: `mvn test` runs the tests, and `mvn package` builds `target/ripple.jar`.

---

## Ground rules (please read before you start)

1. **Follow the class diagram.** Use the same class names and methods as [docs/class-diagram.md](docs/class-diagram.md). If you really need to change something, update the diagram too.
2. **Keep the MVC layers apart:**
   - **Screens** (`view` folder) only show things and call a controller.
   - **Controllers** (`controller` folder) take what the user typed and pass it on.
   - **Database code** lives only in the `repository` folder.
   - **Rules** (checks like "no duplicate names") live in services, never in a button's click code.
3. **Always tell the user what happened.** Every button ends by showing a green or red message in the status bar.
4. **Connect your work in `Main.java`.** Look for the comment `// Section N` with your number and add your lines there. To put your screen in the window, use `frame.addScreen(...)`; there's an example in `MainFrame.java`.
5. **Write tests** for your section's logic, and make sure `mvn test` passes before you finish.
6. **Add a short comment (Javadoc)** on top of every class saying what it does. If the class is part of a pattern, say which one.
7. **Don't commit** `target/`, `data/`, `logs/` or `.env`. The `.gitignore` already handles this.
8. **Stay in scope.** Don't build anything from the "Not in Phase 1" list at the bottom.

---

## Extra client requirements

The assignment asks us to add a few requirements from "the client". We had no real client, so these are **our own assumptions**. Present them that way.

| # | What we assumed the client wants | Section |
|:-:|---|:-:|
| CR1 | No duplicate or empty names in lookup lists ("Zakat" and "zakat " count as the same). | 1 |
| CR2 | You can't delete a lookup value that is already in use. The app says how many records use it. | 1 |
| CR3 | Every donation gets the date and time automatically, plus a receipt number like `RCP-2026-0007`. | 1, 2 |
| CR4 | Only tax-deductible donation types get tax information on their receipt. | 3 |
| CR5 | Each donor picks Email or SMS, and automatically gets a thank-you message that way. Staff can turn this off. | 2, 5 |
| CR6 | Every action shows a clear success or error message. | all |

---

## Section 1 — Foundation, lookup tables, Singleton, Factory

**In short:** set up the base everyone else builds on (database, main data classes, input checks), then make the Lookup Management screen fully work, and add the Singleton and Factory patterns.

**You'll earn:** Lookup tables (0.5) · Singleton (0.5) · Factory (0.5) · CR1, CR2

### Steps

**1. Libraries**
- [x] Add **SQLite** (database driver) and **JUnit 5** (tests) to `pom.xml`.

**2. Database**
- [x] Create `Database`. It opens the database file `data/ripple.db` and creates **all** the tables listed in [architecture.md](docs/architecture.md#6-data-model), so later sections never need to touch it.
- [x] Fill in starting data the first time the app runs:
  - Donation types: *General Fund*, *Zakat*, *Education* (all tax-deductible), and *Food Supplies* (not tax-deductible).
  - Event categories: *Fundraiser*, *Workshop*, *Outreach*.

**3. Main data classes** (the `model` folder)
- [x] Donor, the three donation kinds (Cash, Online, In-kind) and their parent class Donation.
- [x] The two lookup classes: Donation Type (name, description, tax-deductible yes/no) and Event Category (name, description).
- [x] A donation's receipt number looks like `RCP-2026-0007` (year + id).

**4. Input checks**
- [x] Create `InputValidator`, which has small helpers for "is this text filled in and not too long", "is this a valid email or phone", and "is this a number".
- [x] Create `ValidationException`. Its message is what the user sees in red.

**5. Singleton: `ApplicationLogger`**
- [x] Create a single shared logger that writes to `logs/ripple.log` and remembers recent entries for the Dashboard.
- [x] Make sure there can only ever be one: private constructor plus `getInstance()`.

**6. Factory: `DonationFactory`**
- [x] Given "Cash", "Online" or "In-kind", it builds the right donation object and checks the input:
  - amount above 0;
  - online donations need a transaction reference;
  - in-kind donations need an item and a quantity above 0.
- [x] An unknown type such as "Cheque" gives the error *"Unsupported donation method"*.

**7. Lookup Management screen**
- [x] Database code for both lookup tables, plus `LookupService` with the rules:
  - names are 2–50 characters and can't be duplicated, ignoring case and extra spaces (CR1);
  - a value that's in use can't be deleted (CR2).
- [x] `LookupController`, which passes requests from the screen to the service.
- [x] A screen with two tabs, *Donation Types* and *Event Categories*. Each tab has a table and a small form with **Add / Update / Delete / Clear** buttons. Use **one reusable panel class for both tabs**.
- [x] Plug the screen into the window from `Main.java`.

**8. Tests**
- [x] A helper that gives each test its own temporary database.
- [x] Tests for the lookup rules (duplicates, empty names, deleting something in use), the factory, and the logger.

**9. Docs**
- [x] In [class-diagram.md](docs/class-diagram.md), under the Factory and Singleton diagrams, write 2–3 lines: where it is in the code, how to demo it, and which test proves it.

### It works when…
- [x] Lookup Management shows both tabs with the starting data.
- [x] Adding, editing and deleting work, and the changes are still there after restarting the app.
- [x] Adding "zakat" when "Zakat" exists shows a red "already exists" message.
- [x] `mvn test` passes.

### Demo for the instructor
Add "Emergency Relief" → try adding it again (rejected) → rename it → delete it → restart the app to show the data was saved → show the Factory and Singleton code and their tests.

---

## Section 2 — Donations + Observer

**In short:** make the Donations screen work: register donors, record donations, and list them. When a donation is saved, every "observer" that's listening is told about it automatically.

**You'll earn:** Observer (1) · CR3 · CR5 (each donor's preferred channel is saved)

### Steps

**1. Database code**
- [ ] Save and load donors.
- [ ] Save and load donations. When loading, rebuild each donation through the `DonationFactory` from Section 1.

**2. Observer pattern**
- [ ] Two interfaces: `DonationSubject` (attach, detach, notify) and `DonationObserver` (one method: "a donation was recorded").
- [ ] `DonationService` is the subject. When it saves a donation, it notifies all attached observers. If one observer fails, the others still run.
- [ ] A first observer, `DonationLogObserver`, writes a line to the log each time a donation is recorded.

**3. Donor rules** (in `DonationService`)
- [ ] A name is required.
- [ ] If the donor prefers Email they need a valid email; if they prefer SMS they need a valid phone number.

**4. Screen**
- [ ] `DonationController` and the Donations screen, with:
  - a small form to register a donor (name, email, phone, Email/SMS);
  - a form to record a donation (pick the donor, donation type and method). **Only the fields for the chosen method are shown**: amount for Cash; amount and reference for Online; item, quantity and value for In-kind;
  - a table of all donations: receipt no., date, donor, type, method, value.
- [ ] Plug the screen in from `Main.java` and attach the log observer.

**5. Tests**
- [ ] Recording each donation type works.
- [ ] Bad input is rejected, and when it is, no observer is told anything.
- [ ] Attach → notified; detach → not notified; attaching twice → notified only once.

**6. Docs**
- [ ] Under the Observer diagram in [class-diagram.md](docs/class-diagram.md), write 2–3 lines: where it is in the code, how to demo it, and which test proves it.

### It works when…
- [ ] You can register a donor and record one donation of each method; they appear in the table with receipt numbers.
- [ ] Wrong input shows a red message.
- [ ] Each donation adds a line to `logs/ripple.log`.

### Demo for the instructor
Register a donor → record an Online donation → try an In-kind donation with quantity 0 (rejected) → open `logs/ripple.log` to show the observer's line → run the observer tests.

---

## Section 3 — Receipts + Decorator

**In short:** every donation automatically gets a receipt. A basic receipt is "decorated" with extra parts, a thank-you note and, when it applies, tax information, without changing the basic receipt itself.

**You'll earn:** Decorator (1) · CR4

### Steps

**1. Decorator pattern**
- [ ] `Receipt`: an interface with one method that returns the receipt text.
- [ ] `BasicReceipt`: receipt number, date, donor, type, method and value. **No** thank-you or tax logic in here.
- [ ] `ReceiptDecorator`: the parent of all decorators. It wraps another receipt.
- [ ] `ThankYouMessageDecorator`: adds "Thank you, *name*, for your generous support."
- [ ] `TaxInformationDecorator`: adds a tax note with the deductible amount.

**2. Make it automatic**
- [ ] `ReceiptObserver`: a second observer (see Section 2). When a donation is recorded, it builds the receipt (basic + thank-you, plus tax **only if** the donation type is tax-deductible) and saves it.
- [ ] Database code to save and load receipts.

**3. Screen**
- [ ] Add a **View receipt** button to the Donations screen. It opens the selected donation's receipt in a pop-up.
- [ ] Attach the receipt observer in `Main.java`.

**4. Tests**
- [ ] The basic receipt has no thank-you and no tax section.
- [ ] Each decorator adds its part.
- [ ] A *Zakat* donation gets tax info; a *Food Supplies* donation doesn't.

**5. Docs**
- [ ] Under the Decorator diagram in [class-diagram.md](docs/class-diagram.md), write 2–3 lines: where it is in the code, how to demo it, and which test proves it.

### It works when…
- [ ] Every new donation has a receipt you can open.
- [ ] Tax info appears only for tax-deductible types.

### Demo for the instructor
Record a *Zakat* donation → view its receipt (has a tax section) → record a *Food Supplies* donation → view it (no tax section) → show how `ReceiptObserver` wraps the receipt.

---

## Section 4 — Volunteers, Events, Beneficiaries

**In short:** build the three remaining management screens. Each one lets you list, add, edit and delete records. Events also handle attendees and assigned volunteers.

**You'll earn:** three screens of the control panel · MVC

### Steps

**1. Data classes and database code**
- [ ] Volunteer (name, email, phone, skills, availability).
- [ ] Event (name, category, date, location, description) and Attendee (name, email, phone).
- [ ] Beneficiary (name, email, phone, address, needs).
- [ ] Database code for each, including: assign a volunteer to an event, list an event's volunteers, register and list attendees.

**2. Controllers** (with input checks)
- [ ] A name is always required, and an email or phone must be valid if it's filled in.
- [ ] Event dates use the format `2026-12-15`. Every event needs a category.
- [ ] Assigning the same volunteer to the same event twice doesn't create a duplicate.

**3. Screens**
Use the same layout idea as the Lookup screen: a table, a form, and **Add / Update / Delete / Clear** buttons.
- [ ] **Volunteers**, plus an "Assign to event" option.
- [ ] **Events**, with the category picked from the Event Categories lookup, and lists of attendees and assigned volunteers for the selected event.
- [ ] **Beneficiaries.**
- [ ] Plug all three screens in from `Main.java`.

**4. Tests**
- [ ] Add, edit and delete work for all three.
- [ ] Deleting an event also removes its attendees.
- [ ] An event category that's in use can't be deleted.

### It works when…
- [ ] All three screens work, and the data is still there after a restart.
- [ ] Trying to delete a category that's used by an event shows a red message.

### Demo for the instructor
Create an event under *Outreach* → register two attendees → add a volunteer and assign them → try deleting *Outreach* in Lookup Management (blocked) → add, edit and delete a beneficiary.

---

## Section 5 — Messages + Strategy

**In short:** staff can send a message to a whole group by Email or SMS. Each channel is a separate "strategy" that can be swapped while the app runs. Donors also get an automatic thank-you on their preferred channel.

> Sending is **simulated**: messages are printed and logged, not really sent. The screen must say so clearly.

**You'll earn:** Strategy (1) · CR5

### Steps

**1. Strategy pattern**
- [ ] `CommunicationStrategy`: an interface for "a way of sending a message".
- [ ] `EmailStrategy`: uses the person's email.
- [ ] `SmsStrategy`: uses the phone number and rejects messages over 160 characters.
- [ ] `CommunicationService`: holds the current strategy and lets you change it with `setStrategy(...)`. It must **not** contain any "if email… else if SMS…" code.
- [ ] Every attempt, sent or skipped, is saved in a message log.

**2. Automatic thank-you**
- [ ] `DonorNotificationObserver`: a third observer. When a donation is recorded, it picks the donor's preferred channel and sends a short thank-you.
- [ ] Add a **"Notify donors"** checkbox to the Donations screen. Unticking it detaches this observer; ticking it re-attaches it.

**3. Communication screen**
- [ ] Choose who to send to: all donors, all volunteers, all beneficiaries, or the attendees of one event.
- [ ] Choose Email or SMS, write a subject and message, then **Send**.
- [ ] Show a result like *"Sent 4, skipped 2 (no valid phone)"*.
- [ ] Show a table of the message log.
- [ ] Show a visible note: *"Simulated — no real email or SMS is sent."*
- [ ] Plug the screen in from `Main.java` and attach the observer.

**4. Tests**
- [ ] Switching from Email to SMS changes which address is used.
- [ ] People without a valid address are skipped and logged.
- [ ] The thank-you uses each donor's preferred channel, and stops after the observer is detached.

**5. Docs**
- [ ] Under the Strategy diagram in [class-diagram.md](docs/class-diagram.md), write 2–3 lines: where it is in the code, how to demo it, and which test proves it.

### It works when…
- [ ] Sending to each group works with both channels, and the log updates.
- [ ] Recording a donation creates a thank-you in the log, unless "Notify donors" is unticked.

### Demo for the instructor
Send a message to donors by Email → send the same by SMS (different addresses used) → record a donation and show the automatic thank-you → untick "Notify donors", record again, and show no thank-you.

---

## Section 6 — Dashboard + final polish

**In short:** build the Dashboard, check that everything works together, and get the project ready to present.

**You'll earn:** final evidence for Class diagram (2) · MVC (2) · Control panel (1)

### Steps

**1. Dashboard**
- [ ] A Dashboard screen with totals (donors, donations and their total value, volunteers, events, beneficiaries) and a list of recent activity from the logger.
- [ ] Plug it in from `Main.java`. After this, no placeholder screens are left.

**2. Packaging**
- [ ] Make `mvn package` produce a single `ripple.jar` that runs with `java -jar target/ripple.jar` (use the Maven Shade plugin).

**3. Final checks**
- [ ] Add a test that checks the class diagram and the code still match (every class in the diagram exists, and every class in the code is in the diagram).
- [ ] Check that no screen talks to the database directly. This command should print nothing:
  ```bash
  grep -rlE "import (java\.sql|com\.ripple\.repository)" src/main/java/com/ripple/view
  ```
- [ ] Delete `data/ripple.db` and walk through every section's demo in one go. Fix anything that breaks.

**4. Final documents**
- [ ] Make sure [class-diagram.md](docs/class-diagram.md) matches the final code. Export the diagrams as images into `docs/images/`.
- [ ] Under each pattern diagram, add a short presentation note: the problem, how Ripple solves it, and why not just use if/else.
- [ ] Update the README with a screenshot, how to run the app, and a table that maps each grading item to where it is in the app.

### It works when…
- [ ] The Dashboard numbers change after you record a donation.
- [ ] All tests pass and the jar runs on a fresh machine.
- [ ] The README is ready to hand in.

### Demo for the instructor
Start the app fresh → Dashboard shows zeros → walk through Sections 1–5 → return to the Dashboard to show the updated totals and activity.

---

## Not in Phase 1

Please don't build these yet; they belong to Phase 2 or later:

- The Phase 2 patterns: Command, Adapter, Facade, Template Method, Iterator, State, Proxy
- Admin pages, payments, logins
- Volunteer hours and certificates, tracking aid distribution, ticketing, social media
- Real email or SMS sending
