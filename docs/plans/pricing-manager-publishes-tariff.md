---
name: pricing-manager-publishes-tariff
intent: pricing-manager-publishes-tariff
spec: pricing-manager-publishes-tariff
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A pricing manager wants to publish a new tariff

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands entirely in **Pricing**, a context that has a canvas but no code yet. Tariffs are Pricing's own state
(CML responsibility "Tariffs"; canvas Inbound "Pricing manager / Publish Tariff", "Tariffs"). This plan **creates the
Pricing module** (scaffolder, new context) because it comes first in build order among the Pricing intents: the
tiered-tariff intent "follows" this one. R2 (a ride is priced with the tariff valid when it started) is proven here as
"which tariff is valid at a moment"; its examples state a total fee, so their acceptance tests wait for the
Price for Session query that `finance-prices-ride-by-tiered-tariff` builds (S7). No context-map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A pricing manager can publish a complete tariff valid from a future moment | Pricing | Pricing owns tariffs (CML responsibility "Tariffs"; canvas UL "Tariff", Inbound "Publish Tariff") |
| R2 A ride is priced with the tariff valid when it started, so a new tariff never changes a started ride | Pricing | the tariff schedule is Pricing's state; canvas decision "The tariff valid at session start applies"; Settlement only asks for a price (CML `Pricing [U,OHS,PL]->[D,CF] Settlement`) |
| R3 A tariff's valid from must lie in the future when it is published | Pricing | rule that can reject Publish Tariff, reads Pricing's clock and the tariff (canvas Assumptions DE-96) |
| R4 A tariff states every price | Pricing | rule about the tariff's own data (canvas UL "Tariff") |
| R5 Two tariffs can't share the same valid from | Pricing | rule across all tariffs, which only Pricing holds (DE-96) |
| Command: Publish Tariff | Pricing | "who can say no?": R3, R4, R5 are all Pricing's |
| Query: Tariffs | Pricing | "whose data is this a view of?": Pricing's tariffs (canvas Inbound "Pricing manager / Query / Tariffs") |
| Event: Tariff Published | Pricing | the fact is decided in Pricing; no other context listens (integration-contracts.md lists no Pricing event) |

**Outcome:** one existing context (Pricing: canvas exists, module created in S2).

## Context map changes

- None. The Pricing manager is an actor, not a context; the City relationship isn't touched by this intent.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Tariff | Pricing | Tariff | same term (canvas UL) |
| Valid from | Pricing | Valid from (new UL term) | added to the canvas (S1) |
| Launch tariff | Pricing | Launch tariff (new UL term) | added to the canvas (S1) |
| Free period, hour 1/2/3 fee, further hour fee | Pricing | Free period, Hourly fee of billing hour 1/2/3/4+ | REST adapter (`freeMinutes`, `firstHourFee`, `secondHourFee`, `thirdHourFee`, `furtherHourFee`) |
| Drop zone fee, unauthorized zone return fee, penalty | Pricing | Drop zone fee, Unauthorized zone return fee, Penalty | REST adapter (`dropZoneFee`, `unauthorizedZoneFee`, `penalty`) |
| Tariff valid at a moment | Pricing | Tariff valid at (session start) | same meaning; canvas decision "The tariff valid at session start applies" |
| Pricing manager Paula | Pricing | publisher (`publishedBy`) | not in the REST contract: see Risks (Tariff Published "published by") |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Publish Tariff | Pricing | `PublishTariffUseCase` + record `PublishTariff(tariffId, validFrom, freeMinutes, firstHourFee, secondHourFee, thirdHourFee, furtherHourFee, dropZoneFee, unauthorizedZoneFee, penalty)` in `pricing/api/usecases`; primary adapter `POST /pricing/tariffs` (integration-contracts.md); rejections `409 {"error": ..., "message"}` with rule names such as `ValidFromNotInFuture` (R3), `IncompleteTariff` / `NegativeFee` (R4), `DuplicateValidFrom` (R5) |
| Query: Tariffs | Pricing | `TariffsQuery` + result record in `pricing/api/queries`, `TariffsQueryHandler`; `GET /pricing/tariffs` → list of tariffs with valid from and prices |
| Event: Tariff Published | Pricing | domain event `TariffPublished` returned in `DomainEvents` and published by the use case through `DomainEventPublisher`; **no** `api/events` record (no listener outside Pricing) |
| Launch tariff (DE-30, spec vocabulary) | Pricing | seeded at start-up from `infra/config`, valid from `2026-01-01T00:00:00Z` (integration-contracts.md), see D5 |

Data shapes are notes for the tactical skill: valid from is an instant; fees are money in PLN (decimal strings on the
wire); free period is whole minutes on the wire (`freeMinutes`).

## Canvas changes

### Pricing (`docs/bounded-contexts/velocity/pricing/canvas.md`)

- **Ubiquitous language, add:**
  - **Valid from**: the moment from which a tariff applies to newly started sessions; it lies in the future when the tariff is published (DE-96).
  - **Launch tariff**: the tariff VeloCity starts with (DE-30): 15 min free; PLN 3.00, 5.00, 7.00, then 9.00 per hour; drop zone fee PLN 2.00; unauthorized zone return fee PLN 40.00; penalty PLN 150.00; valid from 2026-01-01.
- **Business decisions, add** (the canvas only states the publication rules as assumptions today):
  - `R1 A complete tariff valid from a future moment is published (spec: pricing-manager-publishes-tariff#E1.1)`: Given it is 2026-11-08 10:00, When pricing manager Paula publishes T-2 valid from 2026-12-01 00:00, Then T-2 is published and is listed among the tariffs with its valid from (Tariff Published).
  - `R2 A session that started before a new tariff keeps the old one (spec: pricing-manager-publishes-tariff#E2.1)`: Given T-2 was published valid from 2026-12-01 00:00, When a session from 2026-11-30 23:50 to 2026-12-01 00:30 ending at a station is priced, Then the launch tariff applies and the total fee is PLN 3.00.
  - `R2 A session that started after a new tariff uses it (spec: pricing-manager-publishes-tariff#E2.2)`: Given T-2 (hour 1 PLN 3.50) valid from 2026-12-01 00:00, When a session from 2026-12-01 00:05 to 00:40 ending at a station is priced, Then T-2 applies and the total fee is PLN 3.50.
  - `R3 No retroactive tariff (spec: pricing-manager-publishes-tariff#E3.1)`: Given it is 2026-11-08 10:00 and a session started at 09:30, When Paula publishes T-3 valid from 2026-11-08 09:00, Then the publication is rejected and the session keeps the launch tariff.
  - `R3 Valid from "now" is not in the future (spec: pricing-manager-publishes-tariff#E3.2)`: Given it is 2026-11-08 10:00, When Paula publishes T-3 valid from 2026-11-08 10:00, Then the publication is rejected.
  - `R4 A tariff without a penalty is rejected (spec: pricing-manager-publishes-tariff#E4.1)`: Given it is 2026-11-08 10:00, When Paula publishes T-4 valid from 2026-12-01 00:00 with every price of T-2 except the penalty, Then the publication is rejected.
  - `R4 Fees can't be negative (spec: pricing-manager-publishes-tariff#E4.2)`: Given it is 2026-11-08 10:00, When Paula publishes T-4 valid from 2026-12-01 00:00 with a drop zone fee of PLN -1.00, Then the publication is rejected (DE-96).
  - `R5 Two tariffs can't share a valid from (spec: pricing-manager-publishes-tariff#E5.1)`: Given T-2 was published valid from 2026-12-01 00:00, When on 2026-11-08 at 11:00 Paula publishes T-5 valid from 2026-12-01 00:00, Then the publication is rejected and T-2 stays the tariff valid from that moment.
- The existing decision "The tariff valid at session start applies" stays; it is R2 seen from a session.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style". This intent has no outbound call |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Launch tariff | seeded once at start-up by `infra/config`, valid from `2026-01-01T00:00:00Z`, **not** through `PublishTariffUseCase` (R3 would reject a past valid from) | integration-contracts.md; DE-30. The tactical skill provides a separate way to establish the first tariff of the schedule |
| D6 | R5 is a rule across tariffs | the tactical skill gets R3-R5 as rules of the whole tariff schedule (candidate: one aggregate holding all tariffs, so R5 is checked in one consistency boundary); the choice of building block is the tactical skill's | R5 can't be checked on a single tariff |
| D7 | Examples' clock times | read as UTC instants on the example date (e.g. 2026-11-08 10:00 → `2026-11-08T10:00:00Z`) | integration-contracts.md: times travel as ISO-8601 instants; the spec names no time zone |
| D8 | Tariff timeline in the shared Spring context | every acceptance test that publishes a tariff runs on its own **test day**, shifted from the spec's date with the time of day kept, and after its example publishes a "launch prices again" tariff valid from the next midnight. This plan's test day for T-2 is 2030-04-01 (T-2 valid from `2030-04-01T00:00:00Z`, launch prices again from `2030-04-02T00:00:00Z`). The other Pricing plans use 2030-01-09, 2030-02-09, 2030-02-20 and 2030-03-09 | `@VelocitySpringTest` shares one application context: tariffs published by one test apply to every later ride of every test, and R5 forbids two tests from publishing the same valid from |

## Steps

### S1: Update the Pricing canvas
- **Status:** todo
- **Context:** Pricing
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E1.1, E2.1, E2.2, E3.1, E3.2, E4.1, E4.2, E5.1)
- **Done when:** the canvas contains the UL terms and the eight tagged decisions.

### S2: Scaffold the Pricing module (6a)
- **Status:** todo
- **Context:** Pricing
- **Skill:** bounded-context-scaffolder (new context, 6a; whichever Pricing plan runs first scaffolds, the others use update mode)
- **Covers:** Publish Tariff, Tariffs
- **Done when:** `pricing/package-info.java` with `@BoundedContext(name = "Pricing")` + `@Module`; `api/usecases/PublishTariff`, `api/queries/TariffsQuery` (+ result) records; `PricingArchitectureTest` (only other contexts' `api` used, domain free of Spring) green; `./mvnw test` green.

### S3: Publication rules, test-first (run 1 of 2)
- **Status:** todo
- **Context:** Pricing
- **Skill:** ddd-tactical-building-blocks (D1: loop off; spec examples verbatim as scenarios, one per cycle)
- **Covers:** R1, R3, R5, E1.1, E3.1, E3.2, E5.1, Tariff Published
- **Done when:** one domain test per example (E3.1 here only the rejection); a published tariff yields `TariffPublished`; `./mvnw test` green.

### S4: Complete prices and the tariff valid at a moment, test-first (run 2 of 2)
- **Status:** todo
- **Context:** Pricing
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R4, R2, E4.1, E4.2, E2.1, E2.2
- **Done when:** one domain test per example; E2.1/E2.2 asserted as "the launch tariff / T-2 is the tariff valid at the session start" (the fee itself comes with `finance-prices-ride-by-tiered-tariff`); `./mvnw test` green.

### S5: Use case, query, REST, launch tariff and persistence (6b)
- **Status:** todo
- **Context:** Pricing
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Publish Tariff, Tariffs, Tariff Published
- **Done when:** `PublishTariffUseCase` (load schedule, execute with `Instant.now(velocityClock)`, save, publish via `DomainEventPublisher`); `TariffsQueryHandler`; `POST /pricing/tariffs` and `GET /pricing/tariffs` with the 409/400 mapping; launch tariff seeded (D5); in-memory repository with `Version` (D4); wiring in `pricing/infra/config`; `./mvnw test` green.

### S6: Acceptance tests for publishing
- **Status:** todo
- **Context:** Pricing
- **Skill:** none (implementation-planner)
- **Covers:** E1.1, E3.2, E4.1, E4.2, E5.1
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/PricingManagerPublishesTariffSpecTest.java`, `@VelocitySpringTest`, REST only, `MutableClock` set to the example's time, tariff dates per D8; one method per example; `./mvnw test` green.

### S7: Acceptance tests for the tariff a ride is priced with
- **Status:** todo
- **Context:** Pricing
- **Skill:** none (implementation-planner)
- **Covers:** E2.1, E2.2, E3.1
- **Done when:** prerequisite: plan `finance-prices-ride-by-tiered-tariff` S5 (`GET /pricing/session-price`). Methods added to `PricingManagerPublishesTariffSpecTest`, pricing the example's session through `GET /pricing/session-price` (ending `STATION`) and asserting `tariffId` and `total`; E3.1 also asserts the rejection; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `PricingManagerPublishesTariffSpecTest#e1_1_publishes_complete_tariff_valid_from_future_moment` |
| E2.1 | `PricingManagerPublishesTariffSpecTest#e2_1_ride_started_before_new_tariff_keeps_launch_tariff` |
| E2.2 | `PricingManagerPublishesTariffSpecTest#e2_2_ride_started_after_new_tariff_uses_it` |
| E3.1 | `PricingManagerPublishesTariffSpecTest#e3_1_retroactive_tariff_rejected_and_ride_keeps_launch_tariff` |
| E3.2 | `PricingManagerPublishesTariffSpecTest#e3_2_valid_from_now_is_rejected` |
| E4.1 | `PricingManagerPublishesTariffSpecTest#e4_1_tariff_without_penalty_rejected` |
| E4.2 | `PricingManagerPublishesTariffSpecTest#e4_2_negative_drop_zone_fee_rejected` |
| E5.1 | `PricingManagerPublishesTariffSpecTest#e5_1_second_tariff_with_same_valid_from_rejected` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S5, S6 | |
| R2 / E2.1, E2.2 | S4, S7 | |
| R3 / E3.1 | S3, S7 | |
| R3 / E3.2 | S3, S6 | |
| R4 / E4.1, E4.2 | S4, S6 | |
| R5 / E5.1 | S3, S6 | |

## Risks and open questions

- Spec **Q4** (must the unauthorized zone return fee be higher than the drop zone fee?) is open. The plan enforces only "every price present, none negative" (R4); if the answer is yes, it becomes a fourth publication rule in S4.
- **Published by:** the spec's Tariff Published carries "published by", but `POST /pricing/tariffs` has no publisher field and there is no authentication. The domain event records the publisher as unknown/`null` until identities exist. **CONTRACT GAP:** optional `publishedBy` in `POST /pricing/tariffs` (planned as if it existed; the domain event carries it when given).
- **Missing price → 409, not 400:** the contract reserves 400 for malformed input. A missing price is a business rule here (R4), so it is checked in the domain and answered 409 `IncompleteTariff`; a value that isn't a number at all stays 400.
- Launch tariff seeding bypasses R3 (D5); reviewers should confirm that a start-up tariff isn't "published" in the business sense.
- D8 makes the shared test context safe only if every Pricing test follows it; other plans pricing rides after 2030-01-01 would meet test tariffs.
- Build order across Pricing plans: this plan S1-S6 → `finance-prices-ride-by-tiered-tariff` → this plan S7 → `finance-prices-free-floating-return-by-zone` → `finance-penalizes-overdue-rental`.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Placement single-context and matches CML responsibility "Tariffs"; no integration; every example covered and given an acceptance test; tactical runs ≤ 3 rules; step order ends green (S7 waits for the tiered plan's query); building blocks left to the tactical skill (D6 names only a candidate).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
