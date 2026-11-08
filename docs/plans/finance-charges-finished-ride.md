---
name: finance-charges-finished-ride
intent: finance-charges-finished-ride
spec: finance-charges-finished-ride
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Finance wants every finished ride charged to the rider's card

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Settlement** ("Charging finished rides"; "the only context that moves money"), which this plan
**creates in code** (canvas exists). Settlement listens to Rentals' `RentalSessionEnded` (published language, CML
`Rentals [U,PL]->[D,CF] Settlement`), asks **Pricing** for the fee (`GET /pricing/session-price`, CML
`Pricing [U,OHS,PL]->[D,CF] Settlement`) and asks **Payments** to charge the rider's current card
(`POST /payments/charges`, CML `Payments [U,OHS,PL]->[D,CF] Settlement`). Payments' side of charging (current card,
failure reasons) is built here too, because no other intent owns it (`rider-adds-payment-method` lists charging as out
of scope). Order: Payments (upstream) → Settlement → acceptance tests. Prerequisites: Payments module and current card
from `rider-adds-payment-method` S4; card invalidation from `finance-blocks-rider-with-invalid-card` S4; Price for
Session from `finance-prices-ride-by-tiered-tariff` S5 and `finance-prices-free-floating-return-by-zone` S5.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A finished ride is charged its total fee to the rider's current card right after the ride | Settlement (charge right after the ride, for the ride's fee); Payments (which card: the current one) | Settlement canvas decision "A finished ride is charged its fee", CML "Charging finished rides"; *current card* is Payments' term and state (Payments canvas decision "A charge goes to the current card", DE-5, DE-71) |
| R2 A ride priced at PLN 0.00 is settled without charging the card | Settlement | Settlement canvas decision "A free ride is settled without charging" (DE-37) |
| R3 A failed first charge leaves the ride unsettled, with the next attempt due 5 minutes later | Settlement (attempt count, next attempt due); Payments (the failure and its reason: declined / no valid payment method) | Settlement canvas UL "Charge attempt" (DE-35); Payments canvas decision "A charge without a current card fails" (DE-71) |
| R4 A ride is charged at most once | Settlement | Settlement canvas Assumptions "A ride is settled once, even if Rental Session Ended arrives twice" (DE-95) |
| Command: Charge Ride | Settlement | Settlement decides whether and what to charge (R2, R4); Payments only executes a charge |
| Event: Ride Priced | Pricing | Pricing's calculation (plan `finance-prices-ride-by-tiered-tariff` D5): realised as the Price for Session answer, which Settlement asks for and records as the ride's fee |
| Event: Ride Settled | Settlement | Settlement decides a ride is settled (canvas Outbound "Ride Settled: none outside Settlement yet") |
| Event: Charge Attempt Failed | Settlement | Settlement records the failed attempt and schedules the next one; Payments only reports the failed charge |

**Outcome:** several existing contexts. Hand-offs:

| Upstream | Downstream | Message | Pattern | CML |
|---|---|---|---|---|
| Rentals | Settlement | `RentalSessionEnded` (event) | `@Async @EventListener` ACL listener in Settlement, conformist to Rentals' PL | `Rentals [U,PL]->[D,CF] Settlement` |
| Pricing | Settlement | Price for Session (query) | Settlement secondary port + REST ACL adapter | `Pricing [U,OHS,PL]->[D,CF] Settlement` |
| Payments | Settlement | Charge (command) | Settlement secondary port + REST ACL adapter | `Payments [U,OHS,PL]->[D,CF] Settlement` |

## Context map changes

- None. All three relationships are in the map in the needed direction.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Ride | Settlement | Ride (identified by the session id) | listener `rentalsctxacl` (`RentalSessionEnded.sessionId`) |
| Ending / end reason | Settlement → Pricing | Rentals' `endReason` → Pricing's `ending` (`STATION_RETURN`→`STATION`, `FREE_FLOATING_RETURN`→`FREE_FLOATING`, `LOCK_FAILURE`→`LOCK_FAILURE`, `MAX_RENTAL_TIME_EXCEEDED`→`MAX_RENTAL_TIME`) | Settlement canvas UL "Ending (when asking Pricing)"; adapter `pricingctxacl` |
| Total fee | Settlement | Fee | adapter `pricingctxacl` (`total`) |
| Charge attempt | Settlement | Charge attempt | canvas UL |
| Current card | Payments | Current card (newest verified payment method) | Payments' term; Settlement only names the rider |
| Charge declined / no valid payment method | Payments → Settlement | `failureReason` `DECLINED` / `NO_VALID_PAYMENT_METHOD` → Settlement's failure reason | adapter `paymentsctxacl` |
| Settled | Settlement | Settled | canvas UL |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Charge Ride | Settlement | `ChargeRideUseCase` + record `ChargeRide(sessionId, riderId, startedAt, endedAt, endReason, stationId?, latitude?, longitude?)` in `settlement/api/usecases`; issued by listener `settlement/infra/adapters/primary/rentalsctxacl/RentalSessionEndedListener` (`@AntiCorruptionLayer`, `@Async @EventListener`) translating `rentals.api.events.RentalSessionEnded`; no REST resource (nobody outside issues it) |
| Event: Ride Priced | Pricing → Settlement | `GET /pricing/session-price` via secondary port `RideFees` (Settlement's language: the fee of a finished ride) and adapter `settlement/infra/adapters/secondary/pricingctxacl` (`RestClient` + `VelocityServiceUrls`, own response record); Settlement keeps the fee on its ride; no event |
| Event: Ride Settled | Settlement | domain event `RideSettled(sessionId, riderId, fee, charged, settledAt)` returned in `DomainEvents`, published by the use case via `DomainEventPublisher`; no `api/events` record (no listener) |
| Event: Charge Attempt Failed | Settlement | domain event `ChargeAttemptFailed(sessionId, riderId, fee, attemptNumber, reason, failedAt, nextAttemptDueAt)`; no `api/events` record |
| Charge (Payments side of R1, R3) | Payments | `ChargeCurrentCardUseCase` + record `ChargeCurrentCard(riderId, amount, reference)` in `payments/api/usecases`; `POST /payments/charges` `{riderId, amount, reference}` → `200 {chargeId, status, failureReason?}`; processor port `PaymentProcessor` gains the charge operation, simulated adapter in `paymentprocessoracl` fails `tok_nofunds*` |
| Charge from Settlement | Settlement → Payments | secondary port `RiderCards` (Settlement's language: charge an amount to the rider's card for a reference) and adapter `settlement/infra/adapters/secondary/paymentsctxacl` (`RestClient` + `VelocityServiceUrls`); reference = session id |
| Ride Settlement (query, integration-contracts.md; used by the acceptance tests) | Settlement | `RideSettlementQuery` + result in `settlement/api/queries`, `RideSettlementQueryHandler`; `GET /settlement/rides/{sessionId}` → `{sessionId, riderId, fee, status, failedAttempts}` (+ gaps below) |

## Canvas changes

### Payments (`docs/bounded-contexts/velocity/payments/canvas.md`)

- **Tag existing decisions:** "A charge goes to the current card" ← E1.1; "A charge without a current card fails" ← E3.2.
- **Business decisions, add:**
  - `A declined charge fails with reason declined (spec: finance-charges-finished-ride#E3.1)`: Given Ben's current card is tok_nofunds_0002, When PLN 43.00 is charged for ride S-3 and the processor declines it, Then the charge failed with reason *declined* (Charge Failed).

### Settlement (`docs/bounded-contexts/velocity/settlement/canvas.md`)

- **Ubiquitous language, add:** **Next attempt due at**: the moment of the next charge attempt, 5 minutes after a failed one, while fewer than 3 attempts failed (DE-35).
- **Tag existing decisions:** "A finished ride is charged its fee" ← E1.1; "A free ride is settled without charging" ← E2.1.
- **Business decisions, add:**
  - `R3 A failed first charge schedules the next attempt (spec: finance-charges-finished-ride#E3.1)`: Given Ben's session S-3 ended at 10:30 and is priced at PLN 43.00, When S-3 is charged at 10:30 and Payments reports the charge declined, Then S-3 isn't settled, this was attempt 1 of 3 and the next attempt is due at 10:35 (Charge Attempt Failed).
  - `R3 No current card fails the charge (spec: finance-charges-finished-ride#E3.2)`: Given Carla has no current card and her session S-4 ended at 10:30, priced at PLN 3.00, When S-4 is charged at 10:30, Then the charge fails with *no valid payment method*, this was attempt 1 of 3 and the next attempt is due at 10:35 (Charge Attempt Failed) (DE-71).
  - `R4 A ride is charged at most once (spec: finance-charges-finished-ride#E4.1)`: Given Anna's session S-1 is settled, When Rental Session Ended for S-1 arrives a second time, Then Anna's card isn't charged again and S-1 stays settled (DE-95). (Replaces the assumption line "A ride is settled once…".)

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style": `settlement/.../primary/rentalsctxacl`, `settlement/.../secondary/pricingctxacl`, `settlement/.../secondary/paymentsctxacl`. CML marks Settlement conformist (CF) to all three: the adapters keep the upstream terms in their own records but still sit in ACL packages, the repo's single place for cross-context calls |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Order inside Charge Ride | ask Pricing → record the ride with its fee (a second ride with the same session id is refused by the repository) → charge through Payments (unless the fee is PLN 0.00) → record the attempt's outcome → save → publish | R4 under concurrent `@Async` duplicates: recording before charging makes the second delivery stop before any charge. Ports are called by the use case, never by domain objects |
| D6 | Payments' charging built here | Payments update mode: `ChargeCurrentCardUseCase`, `POST /payments/charges`, processor charge | `rider-adds-payment-method` D6 hands charging to this plan; no other intent owns it |
| D7 | Examples' clock times | UTC instants on the example date (2026-11-09 10:30 → `2026-11-09T10:30:00Z`) | integration-contracts.md: ISO-8601 instants |
| D8 | Acceptance-test arrangement | each test registers its own rider at Accounts (`POST /accounts/riders`, so that later retries in the shared context can block that rider, see `finance-retries-failed-charge`) and uses its own session id; the card is verified directly at Payments (`POST /payments/payment-methods`, `tok_visa_4242_*` / `tok_nofunds_0002_*`), invalidated for E3.2 through `POST /payments/processor-notifications/card-invalidations`; the test publishes Rentals' published-language record `RentalSessionEnded` on the Spring event bus (as Rentals would) with a session the launch tariff prices at the example's fee (S-1 PLN 5.00: 10:00-10:30 free-floating at (52.2298, 21.0123); S-2 PLN 0.00: 10:00-10:12 station; S-3 PLN 43.00: 10:00-10:30 free-floating at (52.2500, 21.1000); S-4 PLN 3.00: 10:00-10:30 station) and waits with Awaitility | Settlement's inbound boundary is that event; Rentals' own tests cover publishing it |

## Steps

### S1: Update the Payments canvas
- **Status:** todo
- **Context:** Payments
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E1.1, E3.1, E3.2)
- **Done when:** canvas carries the two tags and the new decision.

### S2: Charge command in the Payments api (6a)
- **Status:** todo
- **Context:** Payments
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Charge Ride (Payments side)
- **Done when:** prerequisites `rider-adds-payment-method` S4 and `finance-blocks-rider-with-invalid-card` S4 done; `payments/api/usecases/ChargeCurrentCard` record; `PaymentProcessor` port has a charge operation; `PaymentsArchitectureTest` green; `./mvnw test` green.

### S3: Charging the current card, test-first
- **Status:** todo
- **Context:** Payments
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one example per cycle)
- **Covers:** R1, R3, E1.1, E3.1, E3.2
- **Done when:** unit tests: a charge goes to the current card and succeeds; a processor decline fails with *declined*; no current card fails with *no valid payment method* without calling the processor; `./mvnw test` green.

### S4: Charge use case, REST and simulated processor (6b)
- **Status:** todo
- **Context:** Payments
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Charge Ride (Payments side)
- **Done when:** `ChargeCurrentCardUseCase`; `POST /payments/charges` → `{chargeId, status, failureReason?}`; simulated processor fails `tok_nofunds*` charges and records every charge `{token, amount, reference}` for tests; `./mvnw test` green.

### S5: Update the Settlement canvas
- **Status:** todo
- **Context:** Settlement
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E1.1, E2.1, E3.1, E3.2, E4.1)
- **Done when:** canvas carries the UL term, the two tags and the three new decisions.

### S6: Scaffold the Settlement module (6a)
- **Status:** todo
- **Context:** Settlement
- **Skill:** bounded-context-scaffolder (new context, 6a)
- **Covers:** Charge Ride, Ride Priced
- **Done when:** `settlement/package-info.java` with `@BoundedContext(name = "Settlement")` + `@Module`; `api/usecases/ChargeRide`, `api/queries/RideSettlementQuery` (+ result) records; the existing `api/events/RentalFeeUnpaid` and `OutstandingDebtCleared` kept; ports `RideFees`, `RiderCards` with REST adapters in `pricingctxacl`, `paymentsctxacl` (adapter tests against the real endpoints); empty `rentalsctxacl` listener package marked `@AntiCorruptionLayer`; `SettlementArchitectureTest` (only other contexts' `api` used) green; `./mvnw test` green.

### S7: Charging a finished ride, test-first (run 1 of 2)
- **Status:** todo
- **Context:** Settlement
- **Skill:** ddd-tactical-building-blocks (D1: loop off; spec examples verbatim, one per cycle)
- **Covers:** R1, R2, R3, E1.1, E2.1, E3.1, E3.2, Ride Settled, Charge Attempt Failed
- **Done when:** one domain test per example: a successful charge settles the ride; a PLN 0.00 fee settles without a charge; a failed first attempt counts 1 of 3 with the next attempt due 5 minutes later and keeps the reason; `./mvnw test` green.

### S8: At most once, test-first (run 2 of 2)
- **Status:** todo
- **Context:** Settlement
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R4, E4.1
- **Done when:** a domain test shows a settled ride ignores a second charge request without events; `./mvnw test` green.

### S9: Charge Ride use case, listener, query, REST and persistence (6b)
- **Status:** todo
- **Context:** Settlement
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Charge Ride, Ride Priced, Ride Settled, Charge Attempt Failed
- **Done when:** `ChargeRideUseCase` in the D5 order; `RentalSessionEndedListener` (`@Async @EventListener`) → `ChargeRide`; `RideSettlementQueryHandler` + `GET /settlement/rides/{sessionId}`; in-memory ride repository with `Version` that refuses a second ride with the same session id (D4, D5); wiring in `settlement/infra/config` with the `velocityClock`; `./mvnw test` green.

### S10: Acceptance tests
- **Status:** todo
- **Context:** Settlement (with real Rentals event record, Pricing and Payments beans)
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** prerequisites `finance-prices-ride-by-tiered-tariff` S5 and `finance-prices-free-floating-return-by-zone` S5 done. `src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/FinanceChargesFinishedRideSpecTest.java`, `@VelocitySpringTest`, one method per example arranged per D8; the Rentals → Settlement → Pricing → Payments hand-off is asserted with Awaitility on `GET /settlement/rides/{sessionId}` (status, fee, failed attempts, next attempt due, reason) and on the simulated processor's recorded charges (none for E2.1, exactly one for E4.1 after the second event); `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FinanceChargesFinishedRideSpecTest#e1_1_finished_ride_charged_to_current_card_is_settled` |
| E2.1 | `FinanceChargesFinishedRideSpecTest#e2_1_free_ride_settled_without_charging` |
| E3.1 | `FinanceChargesFinishedRideSpecTest#e3_1_declined_first_charge_schedules_next_attempt` |
| E3.2 | `FinanceChargesFinishedRideSpecTest#e3_2_no_current_card_fails_first_charge` |
| E4.1 | `FinanceChargesFinishedRideSpecTest#e4_1_repeated_session_ended_does_not_charge_again` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S7, S9, S10 | |
| R2 / E2.1 | S7, S9, S10 | |
| R3 / E3.1, E3.2 | S3, S4, S7, S9, S10 | |
| R4 / E4.1 | S8, S9, S10 | |

## Risks and open questions

- **CONTRACT GAP:** `POST /payments/charges` lists `failureReason?` without values. Planned as `DECLINED | NO_VALID_PAYMENT_METHOD` (spec Data "reason").
- **CONTRACT GAP:** `GET /settlement/rides/{sessionId}` lacks the next attempt and the failure reason that E3.1/E3.2 assert. Planned as if it also returned `nextAttemptDueAt?` and `lastFailureReason?`.
- **CONTRACT GAP (test seam):** the simulated payment processor must record the charges it received (`token, amount, reference`) so tests can assert "card isn't charged" (E2.1) and "charged once" (E4.1). integration-contracts.md describes only its decline rules; `rider-adds-payment-method` already plans recording for pre-authorizations.
- **Pricing unavailable:** if `GET /pricing/session-price` fails, the listener fails and the ride isn't recorded, so "every ride is eventually settled" isn't guaranteed. Out of the spec's scope; candidate intent or a retry policy later.
- **Duplicate delivery under concurrency:** two `@Async` deliveries of the same `RentalSessionEnded` both ask Pricing; only one may record the ride (D5). The adapter-level reference (= session id) would also let Payments deduplicate, not planned.
- Ride Settled and Charge Attempt Failed stay domain events; if a rider notification or reporting needs them later, an `api/events` record is added then.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Rules split along the canvases (Settlement: when/what/how often; Payments: which card and why it failed); three hand-offs all follow CML directions; upstream Payments before Settlement; tactical runs ≤ 3 rules; every example covered and tested with Awaitility for the async hand-off; no building block chosen (D5 is an ordering/idempotency decision for the use case).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
