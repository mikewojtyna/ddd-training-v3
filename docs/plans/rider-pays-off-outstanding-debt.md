---
name: rider-pays-off-outstanding-debt
intent: rider-pays-off-outstanding-debt
spec: rider-pays-off-outstanding-debt
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to pay off their outstanding debt

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The payoff lands in **Settlement**, which holds the outstanding debt and is the only context that moves money (CML
"Rider balance", "Outstanding debt"; canvas decision "A rider pays off their debt in the app", DE-36). Settlement
charges the whole debt through **Payments** (port from `finance-charges-finished-ride`), publishes
`OutstandingDebtCleared` (listener: Debt Recovery) and asks **Accounts** to lift the *outstanding debt* block
(`DELETE /accounts/riders/{riderId}/blocks/OUTSTANDING_DEBT`, CML `Accounts [U,OHS,PL]->[D,ACL] Settlement`). Lifting
the debt block is built here in Accounts (`finance-blocks-rider-with-invalid-card` leaves it to the debt intents).
The collection behaviour built here is reused by `collections-attempts-debt-collection` (Collect Outstanding Debt).
Prerequisites: `finance-retries-failed-charge` S6 (outstanding debt, Rider Balance, `AccountBlocks` port) and
`finance-blocks-rider-with-invalid-card` S8 (Accounts blocks).

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A rider pays off the whole outstanding debt in one charge, and the debt block is lifted | Settlement (whole debt, one charge, only unpaid rides, DE-95); Accounts (lifting only the *outstanding debt* block, DE-6) | Settlement canvas UL "Collection", decision "A rider pays off their debt in the app"; Accounts canvas decision "A debt unblock doesn't lift a card block", Inbound "Settlement / Unblock Account (reason: outstanding debt)" |
| R2 A failed payment leaves the debt and the block as they were | Settlement | Settlement canvas decision "A failed collection leaves the debt"; nothing is sent to Accounts on failure. Why it fails (declined / no current card) is Payments' (DE-71) |
| R3 Only a rider with outstanding debt can pay it off | Settlement | the rule reads Settlement's debt; "who can say no?" |
| Command: Pay Off Outstanding Debt | Settlement | R3 can reject it; Settlement canvas Inbound "Rider app / Pay Outstanding Debt" |
| Query: Rider's Outstanding Debt | Settlement | Settlement's data (canvas Inbound "Rider app, Debt Recovery / Query / Rider Balance") |
| Event: Outstanding Debt Cleared | Settlement | Settlement decides the debt reached PLN 0.00 (canvas Outbound → Debt Recovery) |
| Event: Account Unblocked | Accounts | the block is lifted in Accounts, which knows whether the rider is now eligible (Accounts canvas UL "Rental eligibility", DE-94) |

**Outcome:** several existing contexts. Hand-offs:

| Upstream | Downstream | Message | Pattern | CML |
|---|---|---|---|---|
| Payments | Settlement | Charge (command) | Settlement port `RiderCards` + REST ACL (from `finance-charges-finished-ride`) | `Payments [U,OHS,PL]->[D,CF] Settlement` |
| Accounts | Settlement | Unblock Account, reason outstanding debt (command) | Settlement port `AccountBlocks` + REST ACL `accountsctxacl` (from `finance-retries-failed-charge`) | `Accounts [U,OHS,PL]->[D,ACL] Settlement` |
| Settlement | Debt Recovery | `OutstandingDebtCleared` (event) | published language in `settlement/api/events` (record exists); listener built by `collections-attempts-debt-collection` | `Settlement [U,S,PL]->[D,C] DebtRecovery` |

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Paying off | Settlement | Collection (charging the whole outstanding debt at once), asked by the rider | canvas UL "Collection"; REST adapter `POST /settlement/riders/{riderId}/debt-payment` |
| Outstanding debt, unpaid rides | Settlement | Outstanding debt, unpaid rides | canvas UL |
| Rider's Outstanding Debt (query) | Settlement | Rider Balance | REST adapter `GET /settlement/riders/{riderId}/balance` |
| Block for outstanding debt lifted | Settlement → Accounts | Unblock with reason `OUTSTANDING_DEBT` | adapter `accountsctxacl` |
| Account Unblocked (eligible to rent now) | Accounts | Account Unblocked (`eligibleNow`) | Accounts' own term (DE-94) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Pay Off Outstanding Debt | Settlement | `PayOffOutstandingDebtUseCase` + record `PayOffOutstandingDebt(riderId)` in `settlement/api/usecases`; `POST /settlement/riders/{riderId}/debt-payment` → `200 {collected, collectedAmount, outstandingDebt}` (a declined charge is `200` with `collected=false`, R2); R3 → `409 {"error": "NoOutstandingDebt", ...}` |
| Query: Rider's Outstanding Debt | Settlement | `RiderBalanceQuery` / `RiderBalanceQueryHandler` / `GET /settlement/riders/{riderId}/balance` built by `finance-retries-failed-charge` S6; after a payoff `outstandingDebt` is `0.00` and `unpaidRides` empty |
| Event: Outstanding Debt Cleared | Settlement | domain event `OutstandingDebtCleared(riderId, amountPaid, clearedAt)`, mapped by the use case to the existing integration event `settlement.api.events.OutstandingDebtCleared {riderId, occurredAt}` and published through `ApplicationEventPublisher` (listener: Debt Recovery) |
| Event: Account Unblocked | Accounts | Settlement's `AccountBlocks` port gains "lift the outstanding debt block" → adapter `accountsctxacl` → `DELETE /accounts/riders/{riderId}/blocks/OUTSTANDING_DEBT`; in Accounts `UnblockAccountUseCase` + record `UnblockAccount(riderId, reason)` in `accounts/api/usecases`, domain event `AccountUnblocked(riderId, reason, eligibleNow, unblockedAt)` (no `api/events` record: no listener) |

## Canvas changes

### Settlement (`docs/bounded-contexts/velocity/settlement/canvas.md`)

- **Tag existing decisions:** "A rider pays off their debt in the app" ← E1.1; "A failed collection leaves the debt" ← E2.1.
- **Business decisions, add:**
  - `R1 The whole debt in one charge (spec: rider-pays-off-outstanding-debt#E1.2)`: Given Ben owes PLN 46.00 from S-3 (PLN 43.00) and S-5 (PLN 3.00) and is blocked for *outstanding debt*, When Ben pays his outstanding debt and Payments charges PLN 46.00 successfully, Then one charge of PLN 46.00 is made, Ben owes PLN 0.00 (Outstanding Debt Cleared) and Settlement unblocks him for *outstanding debt*.
  - `R1 Rides still in their retries aren't part of the payoff (spec: rider-pays-off-outstanding-debt#E1.3)`: Given Ben owes PLN 43.00 from S-3 and attempt 1 for S-5 (PLN 3.00) failed at 10:50, When Ben pays at 10:52 and Payments charges PLN 43.00 successfully, Then only PLN 43.00 is charged, Ben owes PLN 0.00 (Outstanding Debt Cleared), Settlement unblocks him and S-5 stays in its charge retries (DE-95).
  - `R2 No card, no payoff (spec: rider-pays-off-outstanding-debt#E2.2)`: Given Carla owes PLN 9.00, is blocked for *outstanding debt* and *invalid card* and has no current card, When Carla pays her outstanding debt, Then the payment fails, she still owes PLN 9.00 and Settlement doesn't unblock her (DE-6, DE-71).
  - `R3 Nothing to pay off (spec: rider-pays-off-outstanding-debt#E3.1)`: Given Eva owes PLN 0.00, When Eva pays her outstanding debt, Then the payment is rejected and her card isn't charged.

### Accounts (`docs/bounded-contexts/velocity/accounts/canvas.md`)

- **Business decisions, add:**
  - `A cleared debt lifts the debt block (spec: rider-pays-off-outstanding-debt#E1.1)`: Given Anna is blocked for *outstanding debt* only, When Settlement unblocks her for *outstanding debt*, Then the block is lifted and Anna is eligible to rent (Account Unblocked naming *outstanding debt*, eligible: yes) (DE-94).
- The existing decision "A debt unblock doesn't lift a card block" (DE-6) stays as is.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style": `paymentsctxacl` and `accountsctxacl` reused |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Collection is one behaviour for two askers | the Settlement domain behaviour "collect the whole outstanding debt" (one charge, clear on success, unchanged on failure, rejected at PLN 0.00) is built here; `collections-attempts-debt-collection` S2 adds only `CollectOutstandingDebtUseCase` + REST on top of it | Settlement canvas UL "Collection: asked for by Debt Recovery or by the rider"; one rule, one place |
| D6 | Order inside Pay Off Outstanding Debt | load the rider's debt (R3 may reject) → charge the whole amount through `RiderCards` → record the outcome → save → on success publish `OutstandingDebtCleared` and call `AccountBlocks` to lift the debt block | ports are called by the use case, never by domain objects; Accounts is told only after the debt is cleared |
| D7 | Examples' clock times | UTC instants on the example date | integration-contracts.md: ISO-8601 instants |
| D8 | Acceptance-test arrangement | a test fixture `OutstandingDebtFixture` in `src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/` creates real debt: registers the rider at Accounts, verifies a `tok_nofunds_*` card at Payments, publishes `RentalSessionEnded` for sessions the launch tariff prices at the example's amounts and runs `RetryFailedChargesUseCase` with the `MutableClock` until each fee is unpaid (blocked for outstanding debt by then). A payoff that must succeed first verifies a new valid card (DE-5); E2.1 keeps the `tok_nofunds_*` card; E2.2 invalidates Carla's card through Payments (she is then blocked for *invalid card* too); Carla's PLN 9.00 is three unpaid PLN 3.00 rides (no single launch-tariff ride costs PLN 9.00). `collections-attempts-debt-collection`'s `UnpaidRideFixture` should reuse this fixture; whichever plan is built first creates it | real beans end to end; the debt and the block are Settlement's and Accounts' real behaviour |

## Steps

### S1: Update the Settlement canvas
- **Status:** todo
- **Context:** Settlement
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E1.1, E1.2, E1.3, E2.1, E2.2, E3.1)
- **Done when:** canvas carries the two tags and the four new decisions. Prerequisite: plan `finance-retries-failed-charge` S6 done.

### S2: Update the Accounts canvas
- **Status:** todo
- **Context:** Accounts
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tag for E1.1)
- **Done when:** canvas carries the new decision. Prerequisite: plan `finance-blocks-rider-with-invalid-card` S8 done.

### S3: Unblock command in the Accounts api (6a)
- **Status:** todo
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Account Unblocked
- **Done when:** `accounts/api/usecases/UnblockAccount` record; `AccountsArchitectureTest` green; `./mvnw test` green.

### S4: Lifting the debt block, test-first
- **Status:** todo
- **Context:** Accounts
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R1, E1.1, Account Unblocked
- **Done when:** unit tests: lifting *outstanding debt* from a rider blocked only for it records Account Unblocked with eligible yes; with *invalid card* also present, eligible no and the card block stays (canvas decision, DE-6); lifting a reason not in place changes nothing; `./mvnw test` green.

### S5: Unblock use case and REST (6b)
- **Status:** todo
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Account Unblocked
- **Done when:** `UnblockAccountUseCase`; `DELETE /accounts/riders/{riderId}/blocks/OUTSTANDING_DEBT` (`404` unknown rider); Account Status and Rental Eligibility reflect it; `./mvnw test` green.

### S6: Pay-off command and the unblock call in Settlement (6a)
- **Status:** todo
- **Context:** Settlement
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Pay Off Outstanding Debt, Account Unblocked
- **Done when:** `settlement/api/usecases/PayOffOutstandingDebt` record (+ result); `AccountBlocks` port and `accountsctxacl` adapter can lift the *outstanding debt* block (adapter test against S5's endpoint); `SettlementArchitectureTest` green; `./mvnw test` green.

### S7: Collecting the whole outstanding debt, test-first
- **Status:** todo
- **Context:** Settlement
- **Skill:** ddd-tactical-building-blocks (D1: loop off; spec examples verbatim, one per cycle)
- **Covers:** R1, R2, R3, E1.1, E1.2, E1.3, E2.1, E2.2, E3.1, Outstanding Debt Cleared
- **Done when:** one domain test per example: the amount to charge is the whole debt from unpaid rides only (E1.3: a ride in retries isn't included); success clears the debt with Outstanding Debt Cleared and empties the unpaid rides; failure (declined or no current card) leaves debt and events unchanged; PLN 0.00 debt is rejected; `./mvnw test` green.

### S8: Pay-off use case, REST, publishing and persistence (6b)
- **Status:** todo
- **Context:** Settlement
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Pay Off Outstanding Debt, Rider's Outstanding Debt, Outstanding Debt Cleared, Account Unblocked
- **Done when:** `PayOffOutstandingDebtUseCase` in the D6 order; `POST /settlement/riders/{riderId}/debt-payment` with `409 NoOutstandingDebt`; `api/events/OutstandingDebtCleared` published; Rider Balance shows the cleared debt; rider-debt repository with `Version`; wiring in `settlement/infra/config`; `./mvnw test` green.

### S9: Acceptance tests
- **Status:** todo
- **Context:** Settlement (with real Accounts, Payments, Pricing beans)
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/RiderPaysOffOutstandingDebtSpecTest.java`, `@VelocitySpringTest`, one method per example arranged per D8, paying through `POST /settlement/riders/{riderId}/debt-payment`; assertions on the response, `GET /settlement/riders/{riderId}/balance`, the simulated processor's recorded charges (E1.2: exactly one of PLN 46.00; E1.3: PLN 43.00 only; E3.1: none), `GET /settlement/rides/{sessionId}` for S-5 in E1.3 (still pending, 1 failed attempt); the Settlement → Accounts hand-off asserted with Awaitility on `GET /accounts/riders/{riderId}` `blocks` and `GET /accounts/riders/{riderId}/rental-eligibility` (E2.2: both blocks stay); `OutstandingDebtCleared` recorded with `@RecordApplicationEvents`; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderPaysOffOutstandingDebtSpecTest#e1_1_rider_pays_off_debt_and_debt_block_is_lifted` |
| E1.2 | `RiderPaysOffOutstandingDebtSpecTest#e1_2_debt_from_two_rides_paid_in_one_charge` |
| E1.3 | `RiderPaysOffOutstandingDebtSpecTest#e1_3_ride_still_in_retries_is_not_part_of_payoff` |
| E2.1 | `RiderPaysOffOutstandingDebtSpecTest#e2_1_declined_payment_leaves_debt_and_block` |
| E2.2 | `RiderPaysOffOutstandingDebtSpecTest#e2_2_no_current_card_leaves_debt_and_both_blocks` |
| E3.1 | `RiderPaysOffOutstandingDebtSpecTest#e3_1_rider_without_debt_cannot_pay_off` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S4, S5, S7, S8, S9 | |
| R1 / E1.2, E1.3 | S7, S8, S9 | |
| R2 / E2.1, E2.2 | S7, S8, S9 | |
| R3 / E3.1 | S7, S8, S9 | |

## Risks and open questions

- **CONTRACT GAP (minor):** `unpaidRides` item shape in Rider Balance unspecified; planned as `{sessionId, amount}` (same gap as in `finance-retries-failed-charge`).
- **CONTRACT GAP (test seam):** the simulated processor's recorded charges (also listed in `finance-charges-finished-ride`) are needed for "one charge of PLN 46.00" (E1.2) and "card isn't charged" (E3.1).
- The spec's Outstanding Debt Cleared carries "amount paid"; the published `OutstandingDebtCleared` record has only `riderId, occurredAt`. Kept as is: the domain event has the amount, and Debt Recovery doesn't need it.
- What the Ride Settlement query shows for a ride whose unpaid fee was paid off (`UNPAID` or `SETTLED`) isn't specified; the plan leaves the ride's status to the tactical skill and asserts only the rider's balance.
- If the unblock call to Accounts fails after a successful charge, the debt is cleared but the rider stays blocked. No retry is planned; a later block/unblock reconciliation would be a new intent.
- D5 couples this plan with `collections-attempts-debt-collection` S2 (it depends on this plan's S7). If that plan runs first, it must build S7's behaviour instead.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Debt rules in Settlement, block lifting in Accounts (canvas decision DE-6), no reverse dependency (Settlement calls Accounts' OHS, Accounts knows no Settlement events); upstream Accounts steps before Settlement's; tactical runs ≤ 3 rules; every example covered and tested with Awaitility on the Accounts hand-off.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
