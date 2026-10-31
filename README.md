# Ripple — NGO Management System

Ripple is a desktop application that helps a non-profit organization manage donations, volunteers, events, beneficiaries and communication. It is the Phase 1 deliverable of the CSE336 *Software Design Patterns* major task, built to show **MVC** and five design patterns working inside real features: **Strategy, Observer, Decorator, Factory and Singleton**.

> **Status:** Sections 1–3 of 6 are done: database, lookup tables (Lookup Management screen), Singleton, Factory, donations with Observer, and receipts with Decorator. Screens not built yet say which section of [PHASE1.md](PHASE1.md) builds them.

**Stack:** Java 21 · Swing · SQLite · Maven · JUnit 5

---

## Run in 2 minutes

You need **JDK 21+** and **Maven 3.9+** on your `PATH` (check with `java -version` and `mvn -version`). Same on Windows and Linux.

```bash
mvn compile exec:java
```

Run the tests:

```bash
mvn test
```

The app creates `data/ripple.db` (the database) and `logs/ripple.log` (the activity log) in the folder you start it from. A single runnable `ripple.jar` comes in Section 6; until then, start the app with `mvn compile exec:java`.

### Configuration

Ripple has no secrets. The only setting is where the database file is stored:

| Environment variable | Default | Meaning |
|---|---|---|
| `RIPPLE_DB_PATH` | `data/ripple.db` | Path of the SQLite database file |

See [.env.example](.env.example).

---

## Documentation

| Document | What it covers |
|---|---|
| [PHASE1.md](PHASE1.md) | **Start here.** What is done, team rules, requirements (incl. client requirements CR1–CR6), and the 6 section checklists |
| [docs/class-diagram.md](docs/class-diagram.md) | Complete UML class diagram (the team contract) and one diagram per design pattern |
| [docs/architecture.md](docs/architecture.md) | MVC layers, packages, object wiring, sequence diagrams, database schema |

---

## Project structure

```
pom.xml                          Maven build
PHASE1.md                        Work plan and requirements
docs/                            Class diagram and architecture
src/main/java/com/ripple/
  app/            Main — start-up and object wiring
  model/          Domain entities
  factory/        Factory pattern
  observer/       Observer pattern
  receipt/        Decorator pattern
  communication/  Strategy pattern
  logging/        Singleton pattern
  repository/     SQL / persistence
  service/        Business rules
  controller/     MVC controllers
  view/           MVC views (Swing) — MainFrame is the control panel
```
