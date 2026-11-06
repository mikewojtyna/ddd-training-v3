# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Material for a DDD training (v3). It mixes workshop documents with Java example code. There are two case studies:

- **VeloCity**: a city bike rental service.
  - `velocity/`: the docs. `velocity-vision.md` is a domain-expert role-play guide that is deliberately inconsistent; `velocity-context-map.md` is the reference solution; `bounded-context-discovery-session.md` records a bounded-context discovery session.
  - `src/main/cml/VeloCity-*.cml`: the Context Mapper models.
  - `pl.wojtyna.dddtrainingv3.velocity`: the code.
- **CrowdSorcery**: a crowdfunding/investment platform. It was copied from an earlier training as a reference implementation.
  - `crowdsorcery/CrowdSorcery.md`: the vision.
  - `pl.wojtyna.dddtrainingv3.crowdsorcery`: the code.

`archetypes/` holds business-archetype analyses (Neal Cabage's *Business Model Archetypes*) used in the strategic-design part of the training.

## AI-assisted DDD flow

The training demonstrates this chain. Each step has a skill in `.claude/skills/` and produces artifacts that the next step reads:

| Step | Skill | Produces |
|---|---|---|
| 1. Explore the domain (collaborative workshops, done by people) | none | business processes, product vision (e.g. `velocity/`) |
| 2. Find bounded contexts | `discover-bounded-contexts` (optionally `residuality-analysis`) | discovery report, CML context map, one canvas per context |
| 3. Capture business needs | `intent-creator` | `docs/intents/<intent>.md` + story map |
| 4. Specify one intent | `spec-writer` | `docs/specs/<intent>.md`: rules with examples, context-agnostic contract |
| 5. Plan and build it | `implementation-planner`, which drives `bounded-context-scaffolder` and `ddd-tactical-building-blocks` | `docs/plans/<intent>.md`, canvas updates, code, acceptance tests |

- Intent, spec and plan are 1:1:1 and share the intent's file name.
- The intent's status tracks progress: `draft → accepted → specified → planned → delivered`.
- Discovery, spec-writer and the planner each have an independent critic agent review their output before the user sees it.
- Every step stops for human review. Approval is always the user's decision.

## Build & test

The stack is Maven wrapper, Spring Boot 4.1, and Java 25 (`java.version`). The only locally installed JDK is 27.

```bash
./mvnw compile
./mvnw test
./mvnw test -Dtest=BikeStationTest            # single test class
./mvnw test -Dtest='BikeStationTest#test0'    # single test method
```

**Lombok is not used and must not be added.** Lombok 1.18.46 crashes javac on JDK 27 (`ExceptionInInitializerError: com.sun.tools.javac.tree.EndPosTable`). Write accessors and `equals`/`hashCode` by hand.

### Context map generation

- `velocity/contextmap/VelocityContextMap` loads `src/main/cml/VeloCity-Context-Map.cml` with the Context Mapper DSL and writes a PNG to `target/`. It needs Graphviz `dot` on the PATH.
- `velocity/contextmap/VelocityBoundedContexts` includes just bounded contexts, without relationships as an intermediate step.
- `crowdsorcery/contextmap/ContextMapperExample` builds a map in code with `context-map-generator`.
- Run any of these from the IDE as plain `main` classes. The VeloCity ones use Java 25 instance-style `static void main()`.

## Code architecture

There is a single Spring Boot application (`DddTrainingV3Application`) hosting several systems. Each top-level package is a system, e.g. `velocity` or `crowdsorcery`, plus a top-level `common` package shared by all systems. Within a system package, each subpackage implements a bounded context, e.g. `deposit`, except the `common` and `contextmap` packages. A system's `common` package is for code shared between that system's bounded contexts; code shared across systems goes to the top-level `common`. Package layout per bounded context, following CrowdSorcery's `deposit` as the canonical example:

```
<context>/
  package-info.java          @BoundedContext + @Module (jMolecules)
  api/usecases               primary ports (@PrimaryPort), one class per use case
  api/events                 published/integration events
  api/queries                queries handled by the bounded context
  domain/                    aggregates, value objects, domain events, policies - most of the work
  infra/adapters/primary     REST etc. (@PrimaryAdapter)
  infra/adapters/secondary   repositories, brokers, ACLs to other contexts and other technological integrations (@SecondaryAdapter)
  infra/config               Spring @Configuration wiring (domain classes are not Spring beans)
```

Shared conventions:
- **jMolecules annotations** (`jmolecules-ddd`, `jmolecules-hexagonal-architecture`) mark DDD and hexagonal roles: `@AggregateRoot`, `@Entity`, `@ValueObject`, `@PrimaryPort`, `@SecondaryPort`, and so on. Keep using them on new classes.
- **Domain events:**
  - Aggregate methods return `DomainEvents` (top-level `common/domain`) instead of publishing events themselves.
  - Use cases hand those events to `DomainEventPublisher`, a secondary port. Its Spring implementation, `SpringEventPublisher` (`common/infra/spring`), forwards them to `ApplicationEventPublisher`.
  - Both systems reuse these top-level `common` types; don't duplicate them per system.
- **Context integration:**
  - Cross-context calls go through anti-corruption-layer packages annotated with the custom `@AntiCorruptionLayer` package annotation, for example `deposit/infra/adapters/secondary/paymentctxacl` and `investment/transfer/depositctxacl`.
  - The relationships are documented in the CML context maps.

Each system has its own `CLAUDE.md` with system-specific context: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md` and `src/main/java/pl/wojtyna/dddtrainingv3/crowdsorcery/CLAUDE.md`.
