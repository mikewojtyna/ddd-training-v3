# Settlement bounded context: implementation plan

- **System / package**: `velocity.settlement`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/settlement/canvas.md`
- **Last updated**: 2026-11-08
- **Status**: implemented

## Review notes
- 2026-11-08: approved via docs/plans/finance-charges-finished-ride.md, docs/plans/finance-retries-failed-charge.md, docs/plans/rider-pays-off-outstanding-debt.md and docs/plans/collections-attempts-debt-collection.md (S2) (autonomous run, 2026-11-08). Structural decisions and the TDD-loop choice (off, D1 in every plan) come from those plans.
- Decisions the intent plans left open, taken here (autonomous run): see the Decisions table rows marked *(new)*.

## Canvas summary
Settlement makes sure every ride is eventually settled: it charges each finished ride, retries a failed charge (3
attempts, 5 minutes apart), turns what can't be collected into the rider's outstanding debt and makes Accounts block
the rider until the debt is collected or paid off. Core domain, revenue, custom built; roles: Execution Model, Enforcer.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards Rentals (inbound, Rental Session Ended) | yes: `infra/adapters/primary/rentalsctxacl/RentalSessionEndedListener` (`@Async @EventListener`) → `ChargeRideUseCase` | integration-contracts.md; CML `Rentals [U,PL]->[D,CF] Settlement` |
| ACL towards Pricing (Price for Session) | yes: port `domain/ride/RideFees`, adapter `infra/adapters/secondary/pricingctxacl/PricingRideFees` (REST, own response record, end reason → ending translation) | plan `finance-charges-finished-ride` D3 |
| ACL towards Payments (Charge) | yes: port `domain/charging/RiderCards`, adapter `infra/adapters/secondary/paymentsctxacl/PaymentsRiderCards` | plan `finance-charges-finished-ride` D3 |
| ACL towards Accounts (Block / Unblock, outstanding debt) | yes: port `domain/debt/AccountBlocks`, adapter `infra/adapters/secondary/accountsctxacl/AccountsAccountBlocks` (`404` → `UnknownRiderAccountException`) | plan `finance-retries-failed-charge` D3 |
| Boundary exceptions | none | |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | |
| TDD checkpoint loop | off (trainer's autonomy request, 2026-11-08, plans' D1) | domain still built test-first, one example per cycle, red seen before green (see TDD log) |
| Aggregate style | state-based (plans' D2) | |
| Ride vs rider state *(new, plan `finance-retries-failed-charge` D6)* | two aggregates: `RideSettlement` (per ride: fee, status, failed attempts, next attempt due, last failure reason) and `RiderDebt` (per rider: unpaid rides and their fees; the debt is their sum) | R1-R2 of the retries guard one ride; R3 and the collection guard the rider's sum; they share no state |
| How an unpaid ride reaches the debt *(new)* | policy `UnpaidRideFeeBecomesOutstandingDebtPolicy` on the ride's domain event `RideFeeUnpaid` (in-process, synchronous, routed by `infra/event/SettlementLocalEventsHandler`) | one aggregate per transaction; the rule "whenever a fee is unpaid, it becomes debt" is a reaction |
| Block / unblock *(new)* | policies `BlockRiderWithOutstandingDebtPolicy` (on every Rental Fee Unpaid, D5) and `UnblockRiderWhenDebtClearedPolicy` (on Outstanding Debt Cleared); a failing Accounts call is logged by the events handler, the debt stays recorded | plans' D5/D6/D7; Accounts owns "no second notification" |
| Ports called by use cases only | `RideChargeAttempt` (shared by Charge Ride and Retry Failed Charges) and `OutstandingDebtCollector` (shared by Pay Off and Collect) call `RiderCards` between the domain's "what to charge" (`chargeDueAt`, `collection()`) and "record the outcome" (`recordCharge`, `record(collection).as(outcome)`) | plans' D5/D6; repo convention |
| Duplicate Rental Session Ended (DE-95) | the use case ignores a session already recorded; of two concurrent deliveries the repository refuses the second new ride (`Version` initial vs. stored) before anything is charged | plan `finance-charges-finished-ride` D5 |
| Which rides are retried *(new)* | only pending rides with at least one failed attempt (`loadAwaitingRetry`), and only when the ride says the retry is due (`chargeDueAt(now)`) | a ride whose first attempt is still in flight in Charge Ride isn't charged twice by a concurrent retry run |
| Collection racing an unpaid fee *(new)* | a collection carries the unpaid rides it covers; recording it clears only those, and Outstanding Debt Cleared happens only when nothing is left; on a `Version` conflict the collector reloads and records the same (already charged) collection again (up to 3 saves) | the charge can't be undone, so the result must not be lost |
| Collect with nothing owed *(new)* | `POST .../debt-collections` answers `200 {collected: false, collectedAmount: "0.00", outstandingDebt: "0.00"}`; `POST .../debt-payment` answers `409 NoOutstandingDebt` (R3) | plan `collections-attempts-debt-collection` risk: Debt Recovery reads `outstandingDebt = 0` as succeeded |
| Charge reference *(new)* | a ride's charge uses the session id; a collection `DEBT-<riderId>-<uuid>` | Payments keeps no charge history; references only help the processor and tests |
| A paid-off ride's status *(new)* | stays `UNPAID` in Ride Settlement; the rider's balance shows it is paid | plan `rider-pays-off-outstanding-debt` left it open; the ride's own lifecycle ended at unpaid |
| Retry job interval *(new)* | `FailedChargeRetryJob`, `fixedDelay` 30 s (`velocity.settlement.charge-retry-delay-ms`) | due-ness is checked against the clock; the interval only bounds lateness |
| Persistence | `InMemoryRideSettlementRepository`, `InMemoryRiderDebtRepository` (mementos `Snapshot`, `Version` optimistic locking), also the read models of the two queries | plans' D4 |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rentals | Event | Rental Session Ended | `RentalSessionEndedListener` → `ChargeRide` + `ChargeRideUseCase` |
| Finance (clock) | Command | Retry Failed Charges | `RetryFailedCharges` + `RetryFailedChargesUseCase`, triggered by `infra/adapters/primary/FailedChargeRetryJob` |
| Debt Recovery | Command | Collect Outstanding Debt | `CollectOutstandingDebt` + `CollectOutstandingDebtUseCase`; `POST /settlement/riders/{riderId}/debt-collections` → `DebtCollectionResult` |
| Rider app | Command | Pay Outstanding Debt | `PayOffOutstandingDebt` + `PayOffOutstandingDebtUseCase`; `POST /settlement/riders/{riderId}/debt-payment` (`409 NoOutstandingDebt`) |
| Rider app, Debt Recovery | Query | Rider Balance | `RiderBalanceQuery`, `RiderBalanceView`, `RiderBalanceQueryHandler`; `GET /settlement/riders/{riderId}/balance` |
| Tests, support | Query | Ride Settlement | `RideSettlementQuery`, `RideSettlementView`, `RideSettlementQueryHandler`; `GET /settlement/rides/{sessionId}` (`404` unknown) |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Price for Session | Query | Pricing | `domain/ride/RideFees` | `PricingRideFees` (`pricingctxacl`) |
| Charge | Command | Payments | `domain/charging/RiderCards` | `PaymentsRiderCards` (`paymentsctxacl`) |
| Block / Unblock Account (outstanding debt) | Command | Accounts | `domain/debt/AccountBlocks` | `AccountsAccountBlocks` (`accountsctxacl`) |
| Rental Fee Unpaid | Event | Debt Recovery | n/a | `IntegrationEventsPublisher` → `api/events/RentalFeeUnpaid` |
| Outstanding Debt Cleared | Event | Debt Recovery | n/a | `IntegrationEventsPublisher` → `api/events/OutstandingDebtCleared` |
| Ride Settled, Charge Attempt Failed | Event | none outside Settlement | n/a | domain events only (`RideSettlementEvent`) |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Ride | `RideSettlement` (aggregate root), `SessionId` | domain, `RideSettlementView` |
| Fee | `Money` (PLN, `Pln`), `RideFees` | domain, views |
| Charge attempt | `Charge`, `ChargeOutcome`, `ChargeFailureReason`, `RideSettlement#chargeDueAt` / `#recordCharge` | domain |
| Settled | `RideStatus.SETTLED`, `RideSettled` | domain, view |
| Next attempt due at | `RideSettlement` state, `ChargeAttemptFailed.nextAttemptDueAt` | domain, view |
| Rental fee unpaid | `RideFeeUnpaid` (ride), `RiderDebtEvent.RentalFeeUnpaid` (debt), `api.events.RentalFeeUnpaid` | domain, published language |
| Outstanding debt | `RiderDebt` (aggregate root) | domain, `RiderBalanceView` |
| Collection | `DebtCollection`, `RiderDebt#collection` / `#record` | domain |
| Ending (when asking Pricing) | `EndReason` → `PricingRideFees#endingOf` | ACL |
| Outstanding debt cleared | `RiderDebtEvent.OutstandingDebtCleared`, `api.events.OutstandingDebtCleared` | domain, published language |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| A finished ride is charged its fee (charges E1.1) | green | `RideSettlement` | `RideSettlementTest#finishedRideChargedItsFeeIsSettled` |
| A free ride is settled without charging (charges E2.1) | green | `RideSettlement` | `RideSettlementTest#freeRideIsSettledWithoutCharging` |
| A failed first charge schedules the next attempt (charges E3.1, E3.2) | green | `RideSettlement` | `#declinedFirstChargeSchedulesNextAttemptFiveMinutesLater`, `#chargeWithoutValidPaymentMethodSchedulesNextAttempt` |
| A ride is charged at most once (charges E4.1) | green | `RideSettlement` + repository | `#settledRideIsNotChargedAgain`, `InMemorySettlementRepositoriesTest#secondNewRideForSameSessionIsRefused` |
| No retry before it is due; a retry can settle the ride (retries E1.2, E1.1, E1.3) | green | `RideSettlement` | `#noRetryBeforeFiveMinutesPassed`, `#retryFiveMinutesLaterSettlesRide` |
| Second failure schedules the third attempt (retries E2.1) | green | `RideSettlement` | `#secondFailureSchedulesThirdAttempt` |
| Third failure makes the fee unpaid (retries E2.2) | green | `RideSettlement` | `#thirdFailureMakesFeeUnpaid` |
| Unpaid fee becomes outstanding debt (retries E2.2) | green | `RiderDebt`, `UnpaidRideFeeBecomesOutstandingDebtPolicy` | `RiderDebtTest#firstUnpaidFeeBecomesOutstandingDebt`, `OutstandingDebtPoliciesTest#unpaidRideFeeBecomesOutstandingDebt` |
| More unpaid fees add up (retries E3.1) | green | `RiderDebt`, policy | `RiderDebtTest#furtherUnpaidFeeAddsToOutstandingDebt`, `OutstandingDebtPoliciesTest#furtherUnpaidRideFeeAddsToStoredDebt` |
| Unpaid fee blocks the rider, every time (retries E2.2, E3.1; D5) | green | `BlockRiderWithOutstandingDebtPolicy` | `OutstandingDebtPoliciesTest#unpaidFeeBlocksRiderEveryTime` |
| Collected debt is cleared (payoff E1.1, E1.2; canvas "collected debt") | green | `RiderDebt` | `RiderDebtTest#collectedDebtIsCleared`, `#wholeDebtFromSeveralRidesIsCollectedInOneCharge` |
| Cleared debt lifts the debt block (payoff E1.1) | green | `UnblockRiderWhenDebtClearedPolicy` | `OutstandingDebtPoliciesTest#clearedDebtLiftsDebtBlock` |
| A collection clears only the rides it charged (payoff E1.3) | green | `RiderDebt` | `RiderDebtTest#collectionClearsOnlyTheRidesItCharged` |
| A failed payment leaves the debt (payoff E2.1, E2.2) | green | `RiderDebt` | `#declinedPaymentLeavesDebt`, `#paymentWithoutValidCardLeavesDebt` |
| Nothing to pay off (payoff E3.1) | green | `RiderDebt` | `#riderWithoutDebtHasNothingToPayOff` |

### Rules awaiting GWT
- None.

## Deferred until tactical blocks are built
- All generated (see Files).

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| none | | |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/
  package-info.java
  api/events/RentalFeeUnpaid.java, OutstandingDebtCleared.java          (existed)
  api/usecases/ChargeRide(.java|UseCase), RetryFailedCharges(.java|UseCase), PayOffOutstandingDebt(.java|UseCase),
               CollectOutstandingDebt(.java|UseCase), DebtCollectionResult, RideChargeAttempt, OutstandingDebtCollector
  api/queries/RideSettlementQuery, RideSettlementView, RideSettlementsReadModel, RideSettlementQueryHandler,
              RiderBalanceQuery, RiderBalanceView, RiderBalancesReadModel, RiderBalanceQueryHandler
  domain/charging/RiderId, Pln, Charge, ChargeOutcome, ChargeFailureReason, RiderCards, FluentLanguage
  domain/ride/RideSettlement, RideSettlementEvent, RideStatus, SessionId, EndReason, FinishedRide, RideFees,
              RideSettlementRepository
  domain/debt/RiderDebt, RiderDebtEvent, DebtCollection, NoOutstandingDebtException, RiderDebtRepository,
              AccountBlocks, UnknownRiderAccountException, UnpaidRideFeeBecomesOutstandingDebtPolicy,
              BlockRiderWithOutstandingDebtPolicy, UnblockRiderWhenDebtClearedPolicy
  infra/adapters/primary/SettlementController, FailedChargeRetryJob
  infra/adapters/primary/rentalsctxacl/package-info, RentalSessionEndedListener
  infra/adapters/secondary/InMemoryRideSettlementRepository, InMemoryRiderDebtRepository, IntegrationEventsPublisher
  infra/adapters/secondary/pricingctxacl/package-info, PricingRideFees
  infra/adapters/secondary/paymentsctxacl/package-info, PaymentsRiderCards
  infra/adapters/secondary/accountsctxacl/package-info, AccountsAccountBlocks
  infra/event/SettlementLocalEventsHandler
  infra/config/SettlementContextConfig
src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/
  SettlementArchitectureTest, SettlementDriver
  FinanceChargesFinishedRideSpecTest, FinanceRetriesFailedChargeSpecTest, RiderPaysOffOutstandingDebtSpecTest,
  CollectOutstandingDebtRestTest
  domain/ride/RideSettlementTest
  domain/debt/RiderDebtTest, OutstandingDebtPoliciesTest, RecordingAccountBlocks, RecordingEventPublisher
  infra/adapters/secondary/InMemorySettlementRepositoriesTest
  infra/adapters/secondary/pricingctxacl/PricingRideFeesTest
```

## Stubs and follow-ups
- No stubs. Rentals is built in parallel: the tests publish `rentals.api.events.RentalSessionEnded` themselves.
- Debt Recovery (listener of `RentalFeeUnpaid` / `OutstandingDebtCleared`, caller of Collect Outstanding Debt) isn't in code yet.
- If Pricing is unavailable, Charge Ride fails and the ride isn't recorded (plan risk); a technical failure of the first charge call leaves the ride pending with no failed attempt, so it isn't retried (candidate follow-up).
- CML / context map: no change needed.

## Changelog
### 2026-11-08: scaffolded
- Added: everything listed under Files.
- Triggered by: intents `docs/plans/finance-charges-finished-ride.md` (S5-S10), `docs/plans/finance-retries-failed-charge.md` (S1, S3-S7), `docs/plans/rider-pays-off-outstanding-debt.md` (S1, S6-S9), `docs/plans/collections-attempts-debt-collection.md` (S2).
- Canvas: UL "Next attempt due at"; existing decisions tagged with spec IDs; 11 decisions added from the three plans; the assumption "a ride is settled once" replaced by the R4 decision.

#### TDD log (checkpoint loop off, all implemented by Claude)
| # | Scenario | Block | Test | Red seen |
|---|---|---|---|---|
| 1 | charges E1.1 finished ride charged its fee is settled | `RideSettlement#chargeDueAt`, `#recordCharge` | `finishedRideChargedItsFeeIsSettled` | yes (no charge due) |
| 2 | charges E2.1 free ride settled without charging | `RideSettlement.of(...)` | `freeRideIsSettledWithoutCharging` | yes (no Ride Settled) |
| 3 | charges E3.1 declined first charge, next attempt at 10:35 | `#recordCharge` | `declinedFirstChargeSchedulesNextAttemptFiveMinutesLater` | yes (Ride Settled instead of Charge Attempt Failed) |
| 4 | charges E3.2 no valid payment method | `#recordCharge` | `chargeWithoutValidPaymentMethodSchedulesNextAttempt` | no: green at once (cycle 3 keeps the reason) |
| 5 | charges E4.1 settled ride not charged again | `#chargeDueAt` | `settledRideIsNotChargedAgain` | no: green at once (cycle 1's status check) |
| 6 | retries E1.2 no retry at 10:34 | `#chargeDueAt` | `noRetryBeforeFiveMinutesPassed` | no: green at once (cycle 3's due time) |
| 7 | retries E1.1 retry at 10:35 settles | `#chargeDueAt`, `#recordCharge` | `retryFiveMinutesLaterSettlesRide` | no: green at once |
| 8 | retries E2.1 attempt 2, next at 10:40 | `#recordCharge` | `secondFailureSchedulesThirdAttempt` | no: green at once (counting from cycle 3) |
| 9 | retries E2.2 attempt 3 makes the fee unpaid | `#recordCharge` | `thirdFailureMakesFeeUnpaid` | yes (Charge Attempt Failed #3 instead of Ride Fee Unpaid) |
| 10 | retries E2.2 first unpaid fee → debt PLN 43.00 | `RiderDebt#addUnpaidFee` | `firstUnpaidFeeBecomesOutstandingDebt` | yes (no events) |
| 11 | retries E3.1 debt PLN 46.00 | `#addUnpaidFee` | `furtherUnpaidFeeAddsToOutstandingDebt` | no: green at once (debt is the sum since cycle 10) |
| 12 | payoff E1.1, E1.2 collection clears the debt | `#collection`, `#record` | `collectedDebtIsCleared`, `wholeDebtFromSeveralRidesIsCollectedInOneCharge` | yes (empty collection / no events) |
| 13 | payoff E2.1, E2.2 failed payment leaves the debt | `#record` | `declinedPaymentLeavesDebt`, `paymentWithoutValidCardLeavesDebt` | E2.2 yes (collection amount 0); E2.1 green at once |
| 14 | payoff E3.1 nothing to pay off | `#collection` | `riderWithoutDebtHasNothingToPayOff` | yes (no exception) |
| 15 | payoff E1.3 a collection clears only what it charged | `#record` | `collectionClearsOnlyTheRidesItCharged` | yes (Outstanding Debt Cleared emitted) |
| - | optimistic locking, duplicate ride refused | `InMemoryRideSettlementRepository`, `InMemoryRiderDebtRepository` | `InMemorySettlementRepositoriesTest` | written after the domain (persistence after domain) |
| 16 | retries E2.2 / E3.1 policy: unpaid ride fee → debt | `UnpaidRideFeeBecomesOutstandingDebtPolicy` | `unpaidRideFeeBecomesOutstandingDebt`, `furtherUnpaidRideFeeAddsToStoredDebt` | yes (empty `handle`) |
| 17 | retries E2.2 / E3.1 policy: block on every unpaid fee | `BlockRiderWithOutstandingDebtPolicy` | `unpaidFeeBlocksRiderEveryTime` | yes (empty `handle`) |
| 18 | payoff E1.1 policy: cleared debt lifts the block | `UnblockRiderWhenDebtClearedPolicy` | `clearedDebtLiftsDebtBlock` | yes (empty `handle`) |

Deviation: cycles 12-15 were written as one batch (one red run showing each failing for its own reason, then one
implementation), and cycles 16-18 likewise; the other cycles were one test at a time.
