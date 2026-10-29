# Ripple — NGO Management System

Ripple is a desktop application that helps a non-profit organization manage donations, volunteers, events, beneficiaries and communication. It is the Phase 1 deliverable of the CSE336 *Software Design Patterns* major task, and it is built to show **MVC** and five design patterns working inside real features: **Strategy, Observer, Decorator, Factory and Singleton**.

> **Status: step 1 of 6 — skeleton.** The control panel opens and navigates between screens; the screens themselves arrive in steps 2–5 (each one says which step).
> A screenshot will be added once the screens are built.

**Stack:** Java 21 · Swing · SQLite (from step 2) · Maven · JUnit 5 (from step 2)

---

## Run in 2 minutes

You need **JDK 21+** and **Maven 3.9+** on your `PATH` (check with `java -version` and `mvn -version`). This works the same on Windows and Linux.

```bash
mvn compile exec:java
```

Or build a jar and run it:

```bash
mvn package
```

```bash
java -jar target/ripple.jar
```

### Configuration

Ripple has no secrets. The only setting is where the database file is stored (used from step 2):

| Environment variable | Default | Meaning |
|---|---|---|
| `RIPPLE_DB_PATH` | `data/ripple.db` | Path of the SQLite database file |

See [.env.example](.env.example).

---

## Documentation

| Document | What it covers |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Phase 1 scope, rubric, functional and non-functional requirements |
| [docs/client-requirements.md](docs/client-requirements.md) | Extra client requirements CR1–CR6 (assumptions) |
| [docs/architecture.md](docs/architecture.md) | MVC layers, packages, object wiring, sequence diagrams, database |
| [docs/class-diagram.md](docs/class-diagram.md) | Complete UML class diagram with legend |
| [docs/pattern-diagrams.md](docs/pattern-diagrams.md) | One diagram per design pattern + MVC |
| [CHANGELOG.md](CHANGELOG.md) | What changed in each step |

---

## Project structure

```
pom.xml                         Maven build (Java 21, exec + jar plugins)
src/main/java/com/ripple/
  app/          Main — start-up and object wiring
  model/        Domain entities
  factory/      Factory pattern
  observer/     Observer pattern
  receipt/      Decorator pattern
  communication/Strategy pattern
  logging/      Singleton pattern
  repository/   SQL / persistence
  service/      Business rules
  controller/   MVC controllers
  view/         MVC views (Swing) — MainFrame is the control panel
docs/           All documentation and diagrams
```
