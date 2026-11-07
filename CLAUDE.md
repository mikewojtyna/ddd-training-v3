# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

The overview lives in the README, imported here: what the repo is, the case studies, the AI-assisted DDD flow and where its artifacts live, build commands and the code layout.

@README.md

## Rules for Claude

### Build

**Lombok is not used and must not be added.** Lombok 1.18.46 crashes javac on JDK 27 (`ExceptionInInitializerError: com.sun.tools.javac.tree.EndPosTable`). Write accessors and `equals`/`hashCode` by hand. The only locally installed JDK is 27.

### Code

- Keep using jMolecules annotations on new classes.
- Aggregates return `DomainEvents` and never publish them; use cases publish through `DomainEventPublisher`.
- Reuse the top-level `common` types (`DomainEvents`, `DomainEventPublisher`, `@AntiCorruptionLayer`, …). Don't duplicate them per system.
- Cross-context calls go only through `@AntiCorruptionLayer` packages. If you change a relationship, keep the CML context map in sync.
- Domain classes aren't Spring beans; wiring goes into the context's `infra/config`.

### The AI-assisted DDD flow

- Intent, spec and plan are 1:1:1 and share the intent's file name.
- Change statuses only through the scripts, never by hand:
  - `intents.py build`/`check` after any intent change;
  - `specs.py approve <intent>` to approve a spec;
  - `plans.py approve|step|done` for plans.
- Every step stops for the user's review. Approval is always the user's decision, never yours.

### Per-system context

Each system has its own `CLAUDE.md` with system-specific context:
- `src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md`
- `src/main/java/pl/wojtyna/dddtrainingv3/crowdsorcery/CLAUDE.md`
- `src/main/java/pl/wojtyna/dddtrainingv3/orders/CLAUDE.md`
