---
name: finance-retries-failed-charge
intent: finance-retries-failed-charge
spec: finance-retries-failed-charge
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Finance wants a failed ride charge retried before it becomes outstanding debt

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The retry lifecycle and the outstanding debt land in **Settlement** (CML "Charge retries", "Outstanding debt"). A
scheduled job runs *Retry Failed Charges* with "now"; each due ride is charged again through **Payments** (port and
ACL from `finance-charges-finished-ride`). After the third failure Settlement adds the fee to the rider's outstanding
debt, publishes `RentalFeeUnpaid` (listener: Debt Recovery) and asks **Accounts** to block the rider for outstanding
debt (`POST /accounts/riders/{riderId}/blocks`, CML `Accounts [U,OHS,PL]->[D,ACL] Settlement`). The block itself, its
notification and "no second notification" (DE-94) are Accounts' rules, already built generically by
`finance-blocks-rider-with-invalid-card` S8, a prerequisite. Also prerequisite: `finance-charges-finished-ride` S10.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A failed charge is retried 5 minutes after the failed attempt | Settlement (when to retry); Payments (which card: the current one, E1.3) | Settlement canvas decision "A failed charge is retried 5 minutes later", Inbound "Finance (clock) / Retry Failed Charges" (DE-35); current card is Payments' state (DE-5, precedent `finance-charges-finished-ride` R1) |
| R2 After the 3rd failed attempt the fee becomes outstanding debt and the rider is blocked | Settlement (unpaid fee, outstanding debt, the decision to block); Accounts (the block and its notification) | Settlement canvas decision "After the third failed attempt the fee becomes outstanding debt", Domain role "Enforcer"; Accounts canvas UL "Block", Outbound "Block Notification" (DE-7) |
| R3 A further unpaid fee adds to the rider's outstanding debt | Settlement (the sum); Accounts (no second notification for a block already in place) | Settlement UL "Outstanding debt"; Accounts canvas Assumptions DE-94 |
| Command: Retry Failed Charges | Settlement | only Settlement knows which rides are due; issued by Settlement's own clock (canvas Inbound "Finance (clock)") |
| Event: Ride Settled | Settlement | as in `finance-charges-finished-ride` |
| Event: Charge Attempt Failed | Settlement | as in `finance-charges-finished-ride` |
| Event: Rental Fee Unpaid | Settlement | Settlement decides a fee is unpaid (canvas Outbound "Rental Fee Unpaid → Debt Recovery"); Debt Recovery reacts (plan `collections-starts-debt-recovery`) |
| Event: Account Blocked | Accounts | the block is placed in Accounts (Accounts canvas Inbound "Settlement / Block Account (reason: outstanding debt)"; "Accounts knows no Settlement events", CML comment) |

**Outcome:** several existing contexts. Hand-offs:

| Upstream | Downstream | Message | Pattern | CML |
|---|---|---|---|---|
| Payments | Settlement | Charge (command) | Settlement port `RiderCards` + REST ACL (from `finance-charges-finished-ride`) | `Payments [U,OHS,PL]->[D,CF] Settlement` |
| Accounts | Settlement | Block Account, reason outstanding debt (command) | Settlement port `AccountBlocks` + REST ACL `accountsctxacl` | `Accounts [U,OHS,PL]->[D,ACL] Settlement` |
| Settlement | Debt Recovery | `RentalFeeUnpaid` (event) | published language in `settlement/api/events` (record exists); listener built by `collections-starts-debt-recovery` | `Settlement [U,S,PL]->[D,C] DebtRecovery` |

## Context map changes

- None. All relationships exist in the needed direction.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Charge attempt, rental fee unpaid, outstanding debt | Settlement | same terms | Settlement canvas UL |
| Retry Failed Charges (now) | Settlement | Retry Failed Charges | `@Scheduled` job passes `Instant.now(velocityClock)` |
| Block for outstanding debt | Settlement → Accounts | Block with reason `OUTSTANDING_DEBT` | adapter `accountsctxacl` (`{reason: "OUTSTANDING_DEBT"}`) |
| Account Blocked, block notification | Accounts | Account Blocked, Block notification | Accounts' own terms |
| Rental Fee Unpaid (rider, ride, unpaid amount, outstanding debt, unpaid at) | Settlement → Debt Recovery | `RentalFeeUnpaid(riderId, sessionId, amount, outstandingDebt, occurredAt)` | use case maps the domain event to the `api/events` record |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Retry Failed Charges | Settlement | `RetryFailedChargesUseCase` + record `RetryFailedCharges(now)` in `settlement/api/usecases`; triggered by `settlement/infra/adapters/primary/FailedChargeRetryJob` (`@Scheduled`, off when `velocity.scheduling.enabled=false`); no REST resource |
| Event: Ride Settled | Settlement | domain event `RideSettled` (from `finance-charges-finished-ride`); no `api/events` record |
| Event: Charge Attempt Failed | Settlement | domain event `ChargeAttemptFailed` (from `finance-charges-finished-ride`) with attempt number 2; no `api/events` record |
| Event: Rental Fee Unpaid | Settlement | domain event `RentalFeeUnpaid`, mapped by the use case to the existing integration event `settlement.api.events.RentalFeeUnpaid {riderId, sessionId, amount, outstandingDebt, occurredAt}` and published through `ApplicationEventPublisher` (listener: Debt Recovery, integration-contracts.md) |
| Event: Account Blocked | Accounts | Settlement port `AccountBlocks` (Settlement's language: block the rider for outstanding debt) → adapter `settlement/infra/adapters/secondary/accountsctxacl` (`RestClient` + `VelocityServiceUrls`) → `POST /accounts/riders/{riderId}/blocks` `{reason: "OUTSTANDING_DEBT"}` → Accounts' `BlockAccountUseCase`, domain event `AccountBlocked` and the block notification (all from `finance-blocks-rider-with-invalid-card` S8) |
| Rider Balance (query, integration-contracts.md; used by the acceptance tests and by `rider-pays-off-outstanding-debt`) | Settlement | `RiderBalanceQuery` + result in `settlement/api/queries`, `RiderBalanceQueryHandler`; `GET /settlement/riders/{riderId}/balance` → `{riderId, outstandingDebt, unpaidRides: [{sessionId, amount}]}` |

## Canvas changes

### Settlement (`docs/bounded-contexts/velocity/settlement/canvas.md`)

- **Tag existing decisions:** "A failed charge is retried 5 minutes later" ← E2.1; "After the third failed attempt the fee becomes outstanding debt" ← E2.2.
- **Business decisions, add:**
  - `R1 A retry can settle the ride (spec: finance-retries-failed-charge#E1.1)`: Given Ben's session S-3 is priced at PLN 43.00 and attempt 1 failed at 10:30, When failed charges are retried at 10:35 and Payments charges PLN 43.00 successfully, Then S-3 is settled (Ride Settled).
  - `R1 No retry before it is due (spec: finance-retries-failed-charge#E1.2)`: Given attempt 1 for S-3 failed at 10:30, When failed charges are retried at 10:34, Then no attempt is made for S-3.
  - `R1 A retry uses the rider's current card (spec: finance-retries-failed-charge#E1.3)`: Given Carla had no current card, attempt 1 for S-4 (PLN 3.00) failed at 10:30 and her card tok_mc_5555 was verified at 10:33, When failed charges are retried at 10:35 and Payments charges PLN 3.00 successfully, Then S-4 is settled (DE-5).
  - `R3 More unpaid fees add up, without a new block (spec: finance-retries-failed-charge#E3.1)`: Given Ben owes PLN 43.00 and is blocked for *outstanding debt*, and attempts 1 and 2 for S-5 (PLN 3.00) failed at 10:50 and 10:55, When the third attempt fails at 11:00, Then S-5's fee is unpaid, Ben owes PLN 46.00 (Rental Fee Unpaid) and Ben stays blocked; Accounts sends no second notification (DE-94).

### Accounts (`docs/bounded-contexts/velocity/accounts/canvas.md`)

- **Business decisions, add:**
  - `Outstanding debt blocks the account (spec: finance-retries-failed-charge#E2.2)`: Given Ben is activated and not blocked, When Settlement blocks Ben for *outstanding debt*, Then Ben is blocked for *outstanding debt* (Account Blocked) and notified (DE-7).
  - `A debt block already in place sends no second notification (spec: finance-retries-failed-charge#E3.1)`: Given Ben is blocked for *outstanding debt*, When Settlement blocks him for *outstanding debt* again, Then nothing changes and no notification is sent (DE-94).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style": new `settlement/.../secondary/accountsctxacl`; `paymentsctxacl` reused |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Block on every unpaid fee | Settlement asks Accounts to block on **every** Rental Fee Unpaid, not only when the debt rises from PLN 0.00; Accounts ignores a block already in place (DE-94, `finance-blocks-rider-with-invalid-card`) | self-healing if an earlier block call failed; "no second notification" stays Accounts' single rule |
| D6 | Ride vs rider state | the tactical skill gets two kinds of state: per ride (attempts, next attempt due, settled / unpaid) and per rider (outstanding debt, unpaid rides). Candidates only; the building blocks and how an unpaid ride reaches the rider's debt are the tactical skill's choice | R1-R2 guard a ride, R3 guards the rider's sum; the debt is also what `rider-pays-off-outstanding-debt` and Debt Recovery's collection change |
| D7 | One failing ride doesn't stop the run | the use case handles each due ride on its own: a failing port call (Payments, Accounts) is logged and that ride is left for the next run | the shared Spring context and production both contain rides of many riders |
| D8 | Examples' clock times | UTC instants on the example date | integration-contracts.md: ISO-8601 instants |
| D9 | Acceptance-test arrangement | each test registers its own rider at Accounts (`POST /accounts/riders`), verifies the card at Payments for that rider id, publishes `RentalSessionEnded` (a session the launch tariff prices at the example's fee, as in `finance-charges-finished-ride` D8) so attempt 1 happens for real, then sets the `MutableClock` and calls `RetryFailedChargesUseCase` directly (scheduling is off in tests). A retry that must succeed on the same card uses the simulated processor's scripted outcome (CONTRACT GAP below); E1.3 verifies a new card at Payments at 10:33 | real beans end to end; attempt 1 is `finance-charges-finished-ride`'s behaviour, not faked |

## Steps

### S1: Update the Settlement canvas
- **Status:** todo
- **Context:** Settlement
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E1.1, E1.2, E1.3, E2.1, E2.2, E3.1)
- **Done when:** canvas carries the two tags and the four new decisions. Prerequisite: plan `finance-charges-finished-ride` S10 done.

### S2: Update the Accounts canvas
- **Status:** todo
- **Context:** Accounts
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E2.2, E3.1)
- **Done when:** canvas carries the two new decisions. Prerequisite: plan `finance-blocks-rider-with-invalid-card` S8 done (`BlockAccountUseCase`, `POST /accounts/riders/{riderId}/blocks` for both reasons, one notification per newly placed reason).

### S3: Retry command, balance query and the Accounts ACL (6a)
- **Status:** todo
- **Context:** Settlement
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Retry Failed Charges, Account Blocked
- **Done when:** `api/usecases/RetryFailedCharges`, `api/queries/RiderBalanceQuery` (+ result) records; port `AccountBlocks` with adapter in `infra/adapters/secondary/accountsctxacl` (adapter test against the real Accounts endpoint); `SettlementArchitectureTest` green; `./mvnw test` green.

### S4: Retry lifecycle of a ride, test-first (run 1 of 2)
- **Status:** todo
- **Context:** Settlement
- **Skill:** ddd-tactical-building-blocks (D1: loop off; spec examples verbatim, one per cycle)
- **Covers:** R1, R2, E1.1, E1.2, E1.3, E2.1, E2.2, Ride Settled, Charge Attempt Failed
- **Done when:** one domain test per example for the ride's state: due exactly 5 minutes after the failed attempt (10:34 not due, 10:35 due); success settles; attempt 2 fails with next attempt at 10:40; attempt 3 makes the ride unpaid; `./mvnw test` green.

### S5: Outstanding debt, test-first (run 2 of 2)
- **Status:** todo
- **Context:** Settlement
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R2, R3, E2.2, E3.1, Rental Fee Unpaid
- **Done when:** one domain test per example for the rider's debt: first unpaid fee → debt PLN 43.00 with Rental Fee Unpaid; second → PLN 46.00 with Rental Fee Unpaid carrying the new total; `./mvnw test` green.

### S6: Retry use case, scheduled job, balance query, publishing and persistence (6b)
- **Status:** todo
- **Context:** Settlement
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Retry Failed Charges, Ride Settled, Charge Attempt Failed, Rental Fee Unpaid, Account Blocked
- **Done when:** `RetryFailedChargesUseCase` (load rides due at `now` → per ride: charge through `RiderCards` → record outcome → on unpaid: add to the rider's debt, publish `api/events/RentalFeeUnpaid`, call `AccountBlocks` (D5) → save → publish domain events), per-ride isolation (D7); `FailedChargeRetryJob` (`@Scheduled`); `RiderBalanceQueryHandler` + `GET /settlement/riders/{riderId}/balance`; repositories with `Version` and a "due at" lookup; wiring in `settlement/infra/config`; `./mvnw test` green.

### S7: Acceptance tests
- **Status:** todo
- **Context:** Settlement (with real Accounts, Payments, Pricing beans)
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/FinanceRetriesFailedChargeSpecTest.java`, `@VelocitySpringTest`, one method per example arranged per D9; attempt 1 (Rentals → Settlement → Payments) awaited with Awaitility on `GET /settlement/rides/{sessionId}`; after each retry: ride status / failed attempts / next attempt due, `GET /settlement/riders/{riderId}/balance`, the simulated processor's recorded charges (E1.2: none at 10:34; E1.3: token `tok_mc_5555`); the Settlement → Accounts hand-off asserted with Awaitility on `GET /accounts/riders/{riderId}` `blocks` and Accounts' recording `RiderNotifications` adapter (E2.2: one notification; E3.1: still one); `RentalFeeUnpaid` recorded with `@RecordApplicationEvents` (published on the test thread); `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FinanceRetriesFailedChargeSpecTest#e1_1_retry_five_minutes_later_settles_ride` |
| E1.2 | `FinanceRetriesFailedChargeSpecTest#e1_2_no_retry_before_five_minutes` |
| E1.3 | `FinanceRetriesFailedChargeSpecTest#e1_3_retry_charges_newly_verified_card` |
| E2.1 | `FinanceRetriesFailedChargeSpecTest#e2_1_second_failure_schedules_third_attempt_without_debt` |
| E2.2 | `FinanceRetriesFailedChargeSpecTest#e2_2_third_failure_makes_fee_outstanding_debt_and_blocks_rider` |
| E3.1 | `FinanceRetriesFailedChargeSpecTest#e3_1_further_unpaid_fee_adds_to_debt_without_new_notification` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1, E1.2, E1.3 | S4, S6, S7 | |
| R2 / E2.1 | S4, S6, S7 | |
| R2 / E2.2 | S2, S4, S5, S6, S7 | |
| R3 / E3.1 | S2, S5, S6, S7 | |

## Risks and open questions

- **CONTRACT GAP (test seam):** E1.1 needs a retry to succeed on the same card that failed 5 minutes earlier, but the simulated processor decides only by token prefix. Planned as if the simulated processor (Payments `paymentprocessoracl`) let a test script the next charge outcome for a token (e.g. "decline the next charge on `tok_visa_…`"). Fallback without the seam: E1.1 is arranged like E1.3 (a new card), which weakens the example.
- **CONTRACT GAP:** `GET /settlement/rides/{sessionId}` needs `nextAttemptDueAt?` (E2.1 "next attempt due at 10:40"); same gap as in `finance-charges-finished-ride`.
- **CONTRACT GAP (minor):** the item shape of `unpaidRides` in Rider Balance isn't specified; planned as `{sessionId, amount}`.
- **Accounts call failing** after the debt was recorded: the ride is unpaid and the event published, but the rider isn't blocked until the next unpaid fee (D5 retries the block then). A retry of the block call itself isn't planned.
- **Shared test context:** `RetryFailedChargesUseCase` also retries other tests' due rides; D7 keeps that from failing a test, and every test asserts only its own rider and rides. Settlement tests of other plans must register their riders at Accounts (see `finance-charges-finished-ride` D8, `collections-attempts-debt-collection` D8) or the block call returns 404 (logged, D7).
- The scheduled job's interval (e.g. every 30 s) decides how late a retry may happen after its due time; the spec only says "5 minutes after", and "due" is checked against the clock, so a late run still retries.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Retry and debt rules in Settlement, block and notification in Accounts (already generic in `finance-blocks-rider-with-invalid-card`), so no rule is duplicated; Settlement → Accounts and Settlement → Debt Recovery follow the CML; tactical runs carry 2 rules each, split by the state they guard; every example covered and tested with Awaitility on each hand-off.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
