---
name: rider-reserves-bike-at-station      # = intent name = spec name = file name without .md
intent: rider-reserves-bike-at-station    # docs/intents/<intent>.md
spec: rider-reserves-bike-at-station      # docs/specs/<intent>.md
system: velocity
status: draft - awaiting review           # draft - awaiting review | approved | in progress | done | superseded
date: 2026-11-07                          # YYYY-MM-DD of the last plan change
spec_date: 2026-11-06                     # the spec's `date` this plan was built from (drift check)
---

# Implementation plan: A rider wants to reserve a bike at a bike station

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

<!-- 2-4 sentences: where the intent lands (which contexts, existing or new), the integration if any, and the order of work. -->
The intent lands in **Rentals**. Its CML responsibilities already include "Reserve and unlock", and every rule in the spec guards bikes at a station, which Rentals owns. No context map change is needed. Rentals has no canvas yet, so step S1 backfills one from the code and the CML before the reservation rules are added.

## Placement

<!-- One row per spec element: every rule R<n>, every command, query and event. "Why" cites a heuristic from
     references/placement-heuristics.md and a source (canvas, CML responsibility, discovery report, user answer). -->
| Spec element | Owner | Why |
|---|---|---|
| R1 A rider can reserve an available bike | Rentals | owns bike availability at stations (CML responsibilities "Bike availability", "Reserve and unlock") |
| Command: Reserve Bike | Rentals | the context whose rules (R2, R3) can reject it |
| Event: Bike Reserved | Rentals | the fact happens in Rentals |

**Outcome:** one existing context.

## Context map changes

<!-- "None." or the relationships/contexts to add, as a CML snippet, plus validate_cml.sh output once written.
     Never edit the reference CML in place without the user's agreement. -->
- None.

## Translation

<!-- Spec term → term in each owning context's ubiquitous language. Note where the translation happens. -->
| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider | Rentals | Borrower | REST adapter (`riderId` → `BorrowerId`) |
| Reservation | Rentals | Reservation (new term) | added to the canvas |

## Contract mapping

<!-- Every spec command, query and event → the code that realises it in its owning context.
     Use the repo's conventions: use case per command (api/usecases), query + handler (api/queries),
     domain event, integration event in api/events only when another context listens, REST under /<context-kebab>/... -->
| Spec element | Context | Code |
|---|---|---|
| Command: Reserve Bike | Rentals | `ReserveBikeUseCase` + `ReserveBikeCommand`; `POST /rentals/stations/{stationId}/reservations` |
| Event: Bike Reserved | Rentals | domain event `BikeReserved` (in the sealed `BikeRentalEvent` hierarchy); no integration event: no listener outside Rentals |

## Canvas changes

<!-- Per owning context: the diff to apply to docs/bounded-contexts/<system>/<context>/canvas.md.
     Business decisions are the spec's examples copied in canvas GWT form, tagged with their spec IDs. -->
### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`, to be backfilled in S1)

- **Ubiquitous language, add:** **Reservation**: a borrower's hold on one bike at a station for the reservation period.
- **Inbound, add:** Rider app | Command | Reserve bike.
- **Business decisions, add:**
  - `R2 One active reservation per borrower (spec: rider-reserves-bike-at-station#E2.1)`: Given … When … Then …

## Decisions

<!-- Structural decisions the scaffolder and tactical skill will take from here. Approval of this plan approves them. -->
| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | on (recommended) | the user stays in control of every domain step |
| D2 | Aggregate style for new aggregates | left to the tactical skill / state-based / event-sourced | |
| D3 | ACLs | default: one per outbound query/command | |

## Steps

<!-- Ordered so each step ends with green tests. Upstream contexts before downstream ones; within a context:
     canvas → scaffolder (6a) → tactical rules in runs of at most 3 → scaffolder (6b) → acceptance tests.
     Status: todo | in progress | done | skipped. "Covers" lists spec IDs (R<n>, E<n>.<k>, command/query/event names). -->
### S1: Backfill the Rentals canvas
- **Status:** todo
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (canvas only)
- **Covers:** none
- **Done when:** canvas reviewed and saved.

### S2: Add the reservation rules to Rentals (run 1 of 2)
- **Status:** todo
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (TDD checkpoint loop), via bounded-context-scaffolder update mode
- **Covers:** R1, R2, R3, E1.1, E2.1, E3.1, E3.2
- **Done when:** a test per example passes; `./mvnw test` green.

### S3: Acceptance tests for the spec examples
- **Status:** todo
- **Context:** Rentals
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** one use-case-level test per example, named after its ID; `./mvnw test` green.

## Acceptance tests

<!-- Spec example → test. Filled in as tests are written. -->
| Example | Test |
|---|---|
| E1.1 | `RiderReservesBikeAtStationSpecTest#e1_1_rider_reserves_available_bike` |

## Traceability

<!-- Every spec rule and example → step → files. Files are filled in during execution. -->
| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S2, S3 | |

## Risks and open questions

- <Open spec questions accepted as open, and how the plan copes with them.>
- <Differences from the reference context map.>

## Review notes

- 2026-11-07: draft written, awaiting review.

## Changelog

### 2026-11-07: planned
- Built from spec dated 2026-11-06.
