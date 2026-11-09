# DDD training v3

Material for a Domain-Driven Design training: workshop documents plus runnable Java examples. Besides classic strategic and tactical DDD, the training shows how AI can assist each step, from discovering bounded contexts to delivering a requirement as tested code, with a human approving every step.

## Case studies

- **VeloCity**: a city bike rental service, the main exercise.
  - `docs/domains/velocity/`: the docs.
    - `velocity-vision.md` is a domain-expert role-play guide. It's deliberately inconsistent, so participants have to uncover the contradictions.
    - `velocity-context-map.md` is the reference solution of the context-mapping exercise.
    - `bounded-context-discovery-session.md` records a bounded-context discovery session.
  - `src/main/cml/VeloCity-*.cml`: Context Mapper models of the reference map (`VeloCity-Bounded-Contexts.cml`, `VeloCity-Context-Map.cml`).
  - Code: `pl.wojtyna.dddtrainingv3.velocity`.
- **CrowdSorcery**: a crowdfunding/investment platform, copied from an earlier training as a reference implementation. Vision: `docs/domains/crowdsorcery/CrowdSorcery.md`. Code: `pl.wojtyna.dddtrainingv3.crowdsorcery`.
- **Orders**: a small practice system for trying the tactical-design skill on a simple aggregate. Code: `pl.wojtyna.dddtrainingv3.orders`.
  - A recorded Claude Code session shows how its Ordering bounded context was built step by step: canvas, plan review, scaffolding, the TDD checkpoint loop, then persistence. See `docs/bounded-contexts/orders/ordering/conversation.md`, next to that context's `canvas.md` and `plan.md`.

`docs/archetypes/` holds business-archetype analyses (Neal Cabage's *Business Model Archetypes*) used in the strategic-design part.

## The AI-assisted DDD flow

Each step has a Claude Code skill in `.claude/skills/` and produces artifacts that the next step reads.

| Step | Skill | Produces |
|---|---|---|
| 1. Explore the domain (collaborative workshops, done by people) | none | business processes, product vision (e.g. `docs/domains/velocity/`) |
| 2. Find bounded contexts | `discover-bounded-contexts` (optionally `residuality-analysis`) | discovery report, CML context map, one canvas per context |
| 3. Capture business needs | `intent-creator` | `docs/intents/<intent>.md` + story map |
| 4. Specify one intent | `spec-writer` | `docs/specs/<intent>.md`: rules with concrete examples, context-agnostic contract |
| 5. Plan and build it | `implementation-planner`, which drives `bounded-context-scaffolder` and `ddd-tactical-building-blocks` | `docs/plans/<intent>.md`, canvas updates, code, acceptance tests |

- Intent, spec and plan are 1:1:1 and share the intent's file name.
- The intent's status tracks progress: `draft → accepted → specified → planned → delivered`.
- Discovery, the spec writer and the planner each have an independent critic agent review their output before you see it.
- Every step stops for human review. Approval is always yours.
- To run steps 3–5 for a whole system at once, use `system-builder`. It orchestrates the skills above: one domain-expert voice and one contract catalogue, parallel builder agents per bounded context (each using `bounded-context-scaffolder`) in dependency waves, then a communication matrix, journeys, an API-only check and a map-vs-code validation with `discover-bounded-contexts`. It runs autonomously or with you in the loop (expert answers, stage approvals, TDD checkpoints relayed from the builders). Design history: `docs/skills/system-builder/design-history.md`.
- Domain code is built test-first, one Given/When/Then example per cycle. By default each cycle stops so you decide who writes the implementation.

### Where the artifacts live

| Path | Content | Validate with |
|---|---|---|
| `docs/context-discovery/<system>/<date>-<slug>/discovery.md` | discovery report: heuristic trail, Q&A, critic log | `.claude/skills/discover-bounded-contexts/scripts/validate_cml.sh <file.cml>` |
| `docs/bounded-contexts/<system>/<context>/canvas.md`, `plan.md` | Bounded Context Canvas and the scaffolder's per-context plan | |
| `docs/intents/` | one file per intent, `story-map.md`, generated `README.md` (timeline and value stream) | `python3 .claude/skills/intent-creator/scripts/intents.py build` / `check` |
| `docs/specs/<intent>.md` | spec per intent | `python3 .claude/skills/spec-writer/scripts/specs.py check` |
| `docs/plans/<intent>.md` | implementation plan per intent (not the same as a context's `plan.md`) | `python3 .claude/skills/implementation-planner/scripts/plans.py check` |

Each spec example ends up as an acceptance test: `src/test/java/.../<system>/<context>/<IntentCamelCase>SpecTest.java`.

## Getting started

The stack is the Maven wrapper, Spring Boot 4.1 and Java 25 (`java.version`); it also builds on JDK 27.

```bash
./mvnw compile
./mvnw test
./mvnw test -Dtest=BikeReservationTest       # single test class
./mvnw test -Dtest='BikeReservationTest#riderFarAwayIsRejected'    # single test method
```

Context maps can be rendered to PNG (into `target/`) by running these as plain `main` classes from the IDE. The CML-based ones need Graphviz `dot` on the PATH.
- `velocity/contextmap/VelocityContextMap` renders `src/main/cml/VeloCity-Context-Map.cml`.
- `velocity/contextmap/VelocityBoundedContexts` renders only the bounded contexts, without relationships, as an intermediate step.
- `crowdsorcery/contextmap/ContextMapperExample` builds a map in code with `context-map-generator`.

## Exercises

Each workshop exercise lives on its own branch, `exercise/NN-<name>`, with two tags:

- `exNN/start`: where you begin. It holds `EXERCISE.md` (goal, time, steps, when you're done, debrief questions) and,
  for most exercises, materials in `handouts/`.
- `exNN/solution`: the reference answer (answer key, trainer notes or reference code). Look at it only in the debrief.

The exercise branches are cut from master once and never merged back. Master holds the full VeloCity implementation,
so don't use it as a starting point.

### Doing an exercise

```bash
git fetch origin --tags
git switch -c my-ex07 ex07/start      # your own branch from the starting point
cat EXERCISE.md                        # read the task
./mvnw test                            # green before you start (exercises with code)
```

Work and commit on your branch. In the debrief, compare with the reference:

```bash
git diff ex07/start ex07/solution --stat
git diff ex07/start ex07/solution -- docs/specs
```

Exercises 7, 8 and 9 build on each other: each one starts from the previous one's solution. If you didn't finish,
continue from the next start tag (`git switch -c my-ex08 ex08/start`). It contains the reference answer of the step
before.

### The exercises

| # | Module | Branch | What you do |
|---|---|---|---|
| 1 | M1+2 Vibe coding | `exercise/01-spot-the-vibe` | Find the business decisions an AI made silently in a one-prompt implementation (`git show ex01/start`). |
| 2 | M6 Domain exploration | `exercise/02-interview-expert` | Event Storming with a role-played expert, then let AI find the gaps in your board. |
| 3 | M6 Archetypes | `exercise/03-archetype-card-sort` | Sort 12 cards into archetype, entity, design pattern or process. |
| 4 | M6 Archetypes | `exercise/04-archetypes-funnel-plm` | Walk the Archetypes Funnel on a PLM narrative, with AI asking the questions. |
| 5 | M3 Modularization | `exercise/05-find-boundaries` | Find VeloCity's bounded contexts with `discover-bounded-contexts`. |
| 6 | M3 Modularization | `exercise/06-layered-vs-vertical` | Redraw a feature of your own system as bounded contexts and estimate the agent's context. |
| 7 | M4 Implementing a module | `exercise/07-write-spec` | Write the spec of a reservation intent with `spec-writer`. |
| 8 | M4 Implementing a module | `exercise/08-plan` | Plan it with `implementation-planner` and decide where it lives. |
| 9 | M4 Implementing a module | `exercise/09-build` | Build reservation expiry test-first in the TDD checkpoint loop. |
| 10 | M5 Testing | `exercise/10-debate-tests` | Debate who should write the tests. |
| 11 | M5 Testing | `exercise/11-tdd-ping-pong` | TDD ping-pong with the `tdd` skill on a station capacity rule. |
| 12 | M5 Testing | `exercise/12-review` | Review an agent's commit with three planted defects (`git show ex12/start`). |
| 13 | M7 Advanced agent setup | `exercise/13-team-skill` | Build a skill for a recurring task of your team, with evals. |
| 14 | M7 Advanced agent setup | `exercise/14-port-to-your-tool` | Port `CLAUDE.md`, a skill and its critic to Copilot or Cursor. |
| 15 | M7 Advanced agent setup | `exercise/15-living-docs` | Decide which docs an agent should read, and where they live. |

Exercises 1, 2, 3, 4, 6, 10 and 15 need no build: you only need `EXERCISE.md`, `handouts/` and, for Exercise 1, git.
The others need the Java build and an AI coding agent (the skills are in `.claude/skills/`).

## Code layout

A single Spring Boot application (`DddTrainingV3Application`) hosts several systems. Each top-level package is a system (`velocity`, `crowdsorcery`, `orders`), plus a top-level `common` package shared by all of them. Within a system, each subpackage is a bounded context, except `common` (shared by that system's contexts) and `contextmap`.

Every bounded context follows the same ports-and-adapters layout, with CrowdSorcery's `deposit` as the canonical example:

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

Conventions:
- **jMolecules annotations** (`jmolecules-ddd`, `jmolecules-hexagonal-architecture`) mark DDD and hexagonal roles: `@AggregateRoot`, `@Entity`, `@ValueObject`, `@PrimaryPort`, `@SecondaryPort` and so on.
- **Domain events:** aggregate methods return `DomainEvents` (top-level `common/domain`) instead of publishing them. Use cases hand them to `DomainEventPublisher`, whose Spring implementation `SpringEventPublisher` (`common/infra/spring`) forwards them to `ApplicationEventPublisher`.
- **Context integration:** cross-context calls go through anti-corruption-layer packages marked with `@AntiCorruptionLayer`, e.g. `deposit/infra/adapters/secondary/paymentctxacl`. The relationships are documented in the CML context maps, and architecture tests (`<Context>ArchitectureTest`) check that contexts only use each other's `api`.

## Repository map

| Path | What's there |
|---|---|
| `docs/domains/velocity/`, `docs/domains/crowdsorcery/` | the case studies' inputs: vision, reference context map, discovery session, process board |
| `docs/archetypes/` | business-archetype analyses |
| `docs/` (other folders) | the flow's artifacts: canvases, discovery reports, intents, specs, plans; `docs/skills/` holds the skills' design history and evals |
| `src/main/cml/` | Context Mapper models |
| `src/main/java/pl/wojtyna/dddtrainingv3/` | the code, one package per system |
| `.claude/skills/` | the Claude Code skills of the flow |

## For the trainer

- `docs/domains/velocity/velocity-context-map.md` and `src/main/cml/VeloCity-Context-Map.cml` are the solution of the context-mapping exercise. Don't show them to participants who are still discovering contexts.
- The branch `ai-ddd-flow-demo` holds a full worked run of the flow, kept off master so participants don't start from the solution. It contains the discovery example (report, `VeloCity-Context-Map-Discovery.cml`, canvases), the fictional ticket `velocity/briefs/VELO-142-reserve-bike-from-app.md` (that branch keeps the old folder layout), and the run from that ticket to the delivered intent `rider-reserves-bike-at-station`, with the implemented Rentals code. It has one git tag per checkpoint, so you can show any step live and jump to the next: `demo/2-bounded-contexts`, `demo/3-input-ticket`, `demo/3-intents`, `demo/4-spec`, `demo/5-plan`, `demo/5-delivered`.
- Each build step of that run is its own commit between `demo/5-plan` and `demo/5-delivered`.
- The branch `velocity-full-implementation` is the best reference for understanding the whole AI-assisted flow at system scale. It's frozen: it won't be updated or removed.
  - It's an autonomous run of every step for all eight VeloCity contexts, with role-played domain-expert answers.
  - It has one commit per stage:
    1. canvases;
    2. 35 intents and specs;
    3. 35 plans;
    4. the implementation, with REST and in-process events between contexts, and domain, Spring and Awaitility tests.
  - Start with the session record `docs/bounded-contexts/velocity/conversation.md` (prompt, decisions, results), then `domain-expert-answers.md` and `integration-contracts.md` next to it.
  - `ai-ddd-flow-demo` follows one intent with a checkpoint per step. This branch shows many intents and contexts being integrated.
- The branch `velocity-discovery-demo` shows step 2 on its own: a bounded-context discovery session for VeloCity run with `discover-bounded-contexts`. It contains the discovery report (`docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md`), the resulting `VeloCity-Context-Map-Discovery.cml` and one canvas per context, on top of the master of that day. The same discovery output is also part of `ai-ddd-flow-demo` (tag `demo/2-bounded-contexts`).
