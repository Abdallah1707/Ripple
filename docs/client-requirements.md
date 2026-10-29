# Ripple — Additional Client Requirements

> **Important:** no real NGO client was interviewed for Phase 1.
> The requirements below are **proposed assumptions** written by the development team, based on how small NGOs usually work.
> They are labelled CR1–CR6 and should be confirmed with a real client before Phase 2.

The course spec asks us to extend the basic requirements "by collecting some requirements from users". This document records the questions we would ask, the answers we assumed, and where each resulting requirement is implemented.

---

## 1. Questions for the client

| # | Question | Why we ask it |
|---|---|---|
| Q1 | Who maintains lists such as donation types and event categories, and what happens if two people add the same one? | Decides validation rules for lookup tables |
| Q2 | Can a donation type or event category be deleted after it has been used? | Protects historical records |
| Q3 | What must appear on a donation receipt? Do you need a receipt number and the exact date? | Receipt content and audit trail |
| Q4 | Which donations are tax-deductible? Should every receipt show tax information? | Avoids printing wrong tax statements |
| Q5 | How do donors prefer to be contacted? Should they get an automatic thank-you? | Communication channel per donor |
| Q6 | How should staff know whether an action worked? | Feedback in the user interface |

---

## 2. Assumed answers → requirements

| ID | Assumed answer | Requirement | Implemented in (planned) | Verified by (planned) |
|---|---|---|---|---|
| **CR1** | Several staff members edit lists; duplicates like "Zakat" and "zakat " cause confusion. | Lookup names are required, trimmed, 2–50 characters and **unique ignoring case**, on add and on rename. | `LookupService.validate`, `UNIQUE COLLATE NOCASE` in the schema | `LookupServiceTest` (duplicate / blank / rename cases) |
| **CR2** | Old donations and events must keep their category. | A lookup value **in use cannot be deleted**; the message says how many records use it. | `LookupService.delete` + `LookupRepository.countUsages`, foreign keys | `LookupServiceTest` (delete-in-use case) |
| **CR3** | Auditors ask for receipt numbers and dates. | Every donation is **time-stamped automatically** and gets a receipt number `RCP-<year>-<id>`. | `Donation.createdAt`, `Donation.getReceiptNumber` | `DonationServiceTest`, `ReceiptDecoratorTest` |
| **CR4** | Only some donation types are tax-deductible (e.g. not "Food Supplies"). | Tax information is added to a receipt **only for tax-deductible donation types**. | `ReceiptObserver` decides whether to wrap with `TaxInformationDecorator` | `ReceiptDecoratorTest` |
| **CR5** | Some donors read email, others only SMS; everyone should get a thank-you. | Each donor has a **preferred channel**; the automatic thank-you uses it. Staff can switch the automatic thank-you off. | `Donor.preferredChannel`, `DonorNotificationObserver` | `DonorNotificationObserverTest` |
| **CR6** | Staff are not technical. | Every action shows a **clear success or error message** in the status bar. | `Result`, `StatusBar` | Manual demo |

---

## 3. Why these and not others

They are small, fit the Phase 1 scope, and each one exercises something we must demonstrate anyway:

- CR1, CR2 → the lookup CRUD rubric item.
- CR3, CR4 → the Decorator (receipts).
- CR5 → the Observer and the Strategy together.
- CR6 → the MVC split (controllers return a `Result`, views only display it).
