# Ripple — Changelog

One entry per implementation step of Phase 1. Newest first.

## Step 1 — Skeleton

- Maven project `ripple` (Java 21), runnable with `mvn compile exec:java` or `java -jar target/ripple.jar`.
- `Main` (composition root) opens `MainFrame`, the User Control Panel: sidebar with the seven modules, `CardLayout` content area, `StatusBar`.
- Screens not built yet say which step adds them (no fake buttons).
- `Result`, `Refreshable`, `StatusBar` created as drawn in the class diagram.
- `package-info.java` in all 11 packages, describing each package's MVC layer or pattern.
- `README.md`, `.env.example`, this changelog.

## Step 0 — Design

- `docs/requirements.md`, `docs/client-requirements.md`, `docs/architecture.md`, `docs/class-diagram.md`, `docs/pattern-diagrams.md`.
- `.gitignore`.
