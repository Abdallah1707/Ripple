# Ripple — Phase 1 Requirements

> **Ripple** is a desktop management system for a non-profit organization (NGO).
> Course: CSE336 Software Design Patterns — Major Assessment Task, **Phase 1 only**.

This document states what Phase 1 of Ripple must do, where each requirement comes from, and what is deliberately left out.

---

## 1. Source of the requirements

| Source | What it gives us |
|---|---|
| `Major Task SP 2025.docx` (course spec) | The five system components, the design-pattern goals, the deliverables and the Phase 1 grading rubric |
| [client-requirements.md](client-requirements.md) | Extra requirements proposed as **assumptions** (no real client was interviewed) |

---

## 2. Phase 1 grading rubric (10 marks)

| # | Item | Marks | Where it is answered |
|---|---|---|---|
| R1 | Class diagram | 2 | [class-diagram.md](class-diagram.md), [pattern-diagrams.md](pattern-diagrams.md) |
| R2 | Strategy pattern | 1 | Communication module — [pattern-diagrams.md#strategy](pattern-diagrams.md#1-strategy--communication-channels) |
| R3 | Observer pattern | 1 | Donation events — [pattern-diagrams.md#observer](pattern-diagrams.md#2-observer--reacting-to-a-new-donation) |
| R4 | Decorator pattern | 1 | Donation receipts — [pattern-diagrams.md#decorator](pattern-diagrams.md#3-decorator--donation-receipts) |
| R5 | Factory pattern | 0.5 | Creating donations — [pattern-diagrams.md#factory](pattern-diagrams.md#4-factory--creating-donations) |
| R6 | Singleton pattern | 0.5 | Application logger — [pattern-diagrams.md#singleton](pattern-diagrams.md#5-singleton--application-logger) |
| R7 | MVC | 2 | [architecture.md](architecture.md) |
| R8 | User Control Panel | 1 | Main window with sidebar navigation (section 4 below) |
| R9 | Client requirements | 0.5 | [client-requirements.md](client-requirements.md) |
| R10 | CRUD lookup tables | 0.5 | Donation Types and Event Categories (FR-L below) |

A full rubric → code → demo → test table will be in `docs/rubric-mapping.md` once the code exists.

---

## 3. Functional requirements

IDs are used in the other documents and in the test names.

### Donation management (FR-D)
| ID | Requirement |
|---|---|
| FR-D1 | Register a donor with name, email, phone and preferred contact channel (Email or SMS). |
| FR-D2 | Record a donation for a donor with a donation type (from the lookup table) and a method: **Cash**, **Online** or **In-kind**. |
| FR-D3 | Cash: amount. Online: amount + transaction reference. In-kind: item description, quantity, estimated value. |
| FR-D4 | Every recorded donation gets an automatic timestamp and a receipt number. |
| FR-D5 | A receipt is generated for every donation. It always contains the basic details and a thank-you message; it contains tax information only when the donation type is tax-deductible. |
| FR-D6 | When a donation is recorded, the system logs it, generates its receipt and (if enabled) notifies the donor. The notification can be switched off at runtime. |
| FR-D7 | List all donations and view the receipt of any donation. |

### Volunteer management (FR-V)
| ID | Requirement |
|---|---|
| FR-V1 | Add, edit, delete and list volunteers with name, email, phone, skills and availability. |
| FR-V2 | Assign a volunteer to an event. |

### Event management (FR-E)
| ID | Requirement |
|---|---|
| FR-E1 | Add, edit, delete and list events with name, category (from the lookup table), date, location and description. |
| FR-E2 | Register an attendee (name, email, phone) for an event and list an event's attendees and assigned volunteers. |

### Beneficiary management (FR-B)
| ID | Requirement |
|---|---|
| FR-B1 | Add, edit, delete and list beneficiaries with name, email, phone, address and needs. |

### Communication (FR-C)
| ID | Requirement |
|---|---|
| FR-C1 | Send a message to a group: all donors, all volunteers, all beneficiaries, or the attendees of one event. |
| FR-C2 | The sender chooses the channel (Email or SMS) at runtime. |
| FR-C3 | Recipients without a valid address for the chosen channel are skipped and reported. |
| FR-C4 | Every message attempt is stored in a message log and shown in the UI. |
| FR-C5 | Delivery is **simulated** (printed to the console and logged with status `SIMULATED`). The UI says so. A real provider can be added later as one new class. |

### Lookup tables (FR-L)
| ID | Requirement |
|---|---|
| FR-L1 | Full CRUD (list, add, edit, delete) for **Donation Types** (name, description, tax-deductible flag). |
| FR-L2 | Full CRUD for **Event Categories** (name, description). |
| FR-L3 | Names are required, trimmed, 2–50 characters, and unique ignoring case. |
| FR-L4 | A value that is used by existing records cannot be deleted; the user is told how many records use it. |
| FR-L5 | Lookup data is stored in the database and survives restarts. |

### Control panel (FR-UI)
| ID | Requirement |
|---|---|
| FR-UI1 | One main window with a sidebar to reach: Dashboard, Donations, Volunteers, Events, Beneficiaries, Communication, Lookup Management. |
| FR-UI2 | The Dashboard shows totals (donors, donations and their total value, volunteers, events, beneficiaries) and recent activity. |
| FR-UI3 | Every action shows a success or error message in a status bar. |

---

## 4. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-1 | **MVC**: views never contain SQL or business rules; they call controllers only. |
| NFR-2 | **Patterns are real**: each of the five patterns is used by a working feature, not only shown in isolation. |
| NFR-3 | **Persistence**: SQLite file `data/ripple.db`, created and seeded automatically on first run. Path can be changed with the `RIPPLE_DB_PATH` environment variable. |
| NFR-4 | **Portability**: runs on Windows and Linux with Java 21 and Maven. |
| NFR-5 | **Tests**: JUnit 5 tests for the business rules and for each pattern. Tests never use the network. |
| NFR-6 | **Documentation**: every public class has Javadoc; the class diagram matches the code (checked by a test). |
| NFR-7 | **No secrets** in the code or repository. |
| NFR-8 | **Few dependencies**: only `sqlite-jdbc` and `junit-jupiter`. |

---

## 5. Out of scope for Phase 1

These are in the course spec but belong to Phase 2 or later. Ripple does **not** show buttons for them.

| Item | Reason |
|---|---|
| Command, Adapter, Facade, Template Method, Iterator, State, Proxy patterns | Phase 2 rubric |
| Admin pages, payment module, core module management | Phase 2 rubric |
| Volunteer hours tracking and certificates | Not needed to demonstrate Phase 1 items |
| Resource distribution tracking and program-impact evaluation | Not needed to demonstrate Phase 1 items |
| Event ticketing, social-media integration | Not needed to demonstrate Phase 1 items |
| Real email / SMS delivery | No provider credentials; simulated instead (FR-C5) |
| User accounts and login | Not in the Phase 1 rubric |

The design keeps these possible later: e.g. a real SMS provider is one new `CommunicationStrategy`, and volunteer hours is one new table plus one repository.
