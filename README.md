# Ripple — NGO Management System

Ripple is a desktop app that helps a non-profit organization manage its donations, volunteers, events, beneficiaries and messages. It is our Phase 1 submission for the CSE336 *Software Design Patterns* major task. It uses **MVC** and five design patterns inside real features: **Strategy, Observer, Decorator, Factory Method and Singleton**.

![The Ripple Dashboard: totals for each module and the recent activity from the log](docs/images/dashboard.png)

**Stack:** Java 21 · Swing with [FlatLaf](https://www.formdev.com/flatlaf/) · SQLite · Maven · JUnit 5

---

## Team

| Name | ID |
|---|---|
| Abdallah Belal Momen | 22P0036 |
| Habeba Adel Sallam | 22P0259 |
| Nouran Mokhtar ElSayed | 22P0254 |
| Sohila Ebrahim Glal | 22P0244 |
| Sandra Ghayes Zarif | 22P0300 |
| Mireille Maher Kamel | 22P0220 |

---

## How to run

You need **JDK 21+** and **Maven 3.9+** (check with `java -version` and `mvn -version`). It works the same on Windows and Linux.

Run the app:

```bash
mvn compile exec:java
```

Run the tests:

```bash
mvn test
```

Build one jar and run it. The jar includes the SQLite driver, so it only needs Java 21 to run:

```bash
mvn package
java -jar target/ripple.jar
```

On first run the app creates `data/ripple.db` (the database, with some starting lookup data) and `logs/ripple.log` (the activity log) in the folder you start it from. Delete `data/ripple.db` to start over with an empty database.

### Configuration

There is only one setting, and it is optional:

| Environment variable | Default | Meaning |
|---|---|---|
| `RIPPLE_DB_PATH` | `data/ripple.db` | Where the SQLite database file is stored |

See [.env.example](.env.example).

---

## The control panel

The main window (`MainFrame`) has a sidebar with one button per screen. The status bar at the bottom shows a green message when an action works and a red one when it doesn't.

| Screen | What you can do |
|---|---|
| Dashboard | See the totals (donors, donations and their total value, volunteers, events, beneficiaries) and the recent activity from the log |
| Donations | Two tabs. *Donors*: add, edit and delete donors (a donor with donations can't be deleted). *Donations*: record Cash, Online, In-kind or Cheque donations, mark them Urgent, Anonymous or a gift "in honor of" someone, view each donation's receipt, and turn the automatic donor thank-you on or off. This is the complete module, see its [CRUD matrix](docs/crud-matrix.md). |
| Volunteers | Add, edit and delete volunteers, and assign a volunteer to an event |
| Events | Add, edit and delete events, register attendees, and see each event's attendees and volunteers. Attendees get a message when an event's date or location changes |
| Beneficiaries | Add, edit and delete beneficiaries |
| Communication | Send a message to a group (donors, volunteers, beneficiaries or an event's attendees) by Email, SMS or Social media, and read the message log. Sending is **simulated**, nothing is really sent |
| Lookup Management | Add, edit and delete Donation Types and Event Categories |

---

## Where to find each grading item

| Grading item | Marks | In the app | In the code | Tests |
|---|:-:|---|---|---|
| Class diagram | 1 | Picture: [docs/images/class-diagram.png](docs/images/class-diagram.png) ([SVG](docs/images/class-diagram.svg)) | [docs/class-diagram.md](docs/class-diagram.md) | `DiagramConsistencyTest` |
| Strategy | 0.5 | Communication screen: send by Email, then by SMS or Social | `communication/` (`CommunicationService.setStrategy`) | `CommunicationServiceTest` |
| Observer | 0.5 | Donations screen: record a donation, then untick "Notify donors". Events screen: change an event's date and its attendees get a message | `observer/`, `service/DonationService.java`, `service/EventService.java` | `DonationServiceTest`, `DonorNotificationObserverTest`, `EventServiceTest`, `AttendeeNotificationObserverTest` |
| Decorator | 0.5 | Donations screen: *View receipt*, and record a donation with Urgent / Anonymous / "In honor of" | `receipt/`, `observer/ReceiptObserver.java`, `extras/` | `ReceiptDecoratorTest`, `ReceiptObserverTest`, `DonationDecoratorTest` |
| Factory | 0.5 | Donations screen: record a donation with each method | `factory/DonationFactory.java` and its 4 subclasses | `DonationFactoryTest` |
| Singleton | 0.5 | Dashboard "Recent activity" and `logs/ripple.log` | `logging/ApplicationLogger.java` | `ApplicationLoggerTest` |
| MVC structure | 0.5 | Every screen | `view` → `controller` → `service` / `repository` / `model` ([architecture](docs/architecture.md)) | `ArchitectureRulesTest` |
| CRUD Lookup tables | 0.5 | Lookup Management screen | `service/LookupService.java` | `LookupServiceTest` |
| client requirements | 0.5 | See the next table | — | — |

---

## Client requirements (our own assumptions)

We had no real client, so these requirements are **our own assumptions**.

| # | Requirement | Where to see it |
|:-:|---|---|
| CR1 | No duplicate or empty names in lookup lists ("Zakat" and "zakat " count as the same) | Lookup Management: add "zakat" while "Zakat" exists and you get a red message. `LookupServiceTest` |
| CR2 | A lookup value that is in use can't be deleted, and the app says how many records use it | Lookup Management: delete "Zakat" after recording a Zakat donation. `LookupServiceTest` |
| CR3 | Every donation gets the date and time automatically, plus a receipt number like `RCP-2026-0007` | The donations table. `DonationFactoryTest`, `DonationTest` |
| CR4 | Only tax-deductible donation types get tax information on their receipt | *View receipt* on a Zakat donation vs. a Food Supplies donation. `ReceiptObserverTest` |
| CR5 | Each donor picks Email, SMS or Social and automatically gets a thank-you that way. Staff can turn this off | Donor form, the "Notify donors on new donation" checkbox, and the Communication message log. `DonorNotificationObserverTest` |
| CR6 | Every action shows a clear success or error message | The status bar on every screen. Every controller returns a `Result` |

---

## Documentation

| Document | What it covers |
|---|---|
| [docs/class-diagram.md](docs/class-diagram.md) | The full UML class diagram, plus one small diagram per design pattern |
| [docs/architecture.md](docs/architecture.md) | MVC layers, packages, how objects are connected, sequence diagrams and the database schema |
| [docs/crud-matrix.md](docs/crud-matrix.md) | CRUD matrix for the Donations module |

---

## Project structure

```
pom.xml                          Maven build (also builds the runnable jar)
docs/                            Class diagram, architecture, CRUD matrix, images
src/main/java/com/ripple/
  app/            Main: starts the app and connects the objects
  model/          Domain classes
  factory/        Factory Method pattern
  observer/       Observer pattern
  receipt/        Decorator pattern (receipts)
  extras/         Decorator pattern (donation extras: urgent, gift, anonymous)
  communication/  Strategy pattern
  logging/        Singleton pattern
  repository/     Database code
  service/        Business rules
  controller/     MVC controllers
  view/           MVC views (Swing), MainFrame is the control panel
src/test/java/com/ripple/         Unit tests, including DiagramConsistencyTest and ArchitectureRulesTest
```
