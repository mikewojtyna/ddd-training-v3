# Settlement bounded context: implementation plan

- **System / package**: `velocity.settlement`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/settlement/canvas.md` (saved from inline input, 2026-11-01)
- **Last updated**: 2026-11-01
- **Status**: implemented

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-01: draft written from the inline description, awaiting review. Open points: the retry rule needs a strict Given/When/Then (Q1-Q4), the "no ACL towards Payments" decision needs a final confirmation after the pushback (D1), and how Settlement learns that a charge failed (Q5).
- 2026-11-01 (review round 1, approved by the user in the conversation): "Otherwise the plan is approved, go ahead." The user supplied two strict GWT rules for the retry decision:
  1. Given a priced ride of 12.50 PLN with 2 failed charge attempts, when the 3rd charge attempt fails, then an Outstanding Debt of 12.50 PLN is recorded and Debt Recorded is published.
  2. Given a priced ride of 12.50 PLN with 1 failed charge attempt, when the 2nd charge attempt succeeds, then the ride is settled and no Outstanding Debt is recorded.

  Changes made as a result:
  - The two rules are added to the canvas's Business Decisions (next to the original prose) and are modelled. That builds the "proposed shape once the rule is confirmed" from the previous draft: the `Settlement` aggregate, `ChargeAttempt`, `OutstandingDebt`, the `SettlementEvent` hierarchy, the repository, `IntegrationEventsPublisher` and `SettlementTest`.
  - Answered by the rules: **Q1** = 3 attempts in total (debt after the 3rd failure). **Q3** = all-or-nothing in the confirmed examples: Outstanding Debt equals the full ride price.
  - **D1 confirmed**: conformist to Payments, no ACL (the plan was approved with D1 as written).
  - Still open: **Q2** (timing), **Q4** (zero price), **Q5** (how the outcome reaches Settlement), plus the new **Q6** (see "Rules awaiting GWT"). My earlier timing suggestions (+5 min, +35 min) were *not* confirmed, so retry scheduling isn't modelled.
  - One addition needed to reach the aggregate while Q5 is open: `RecordChargeAttemptOutcomeUseCase` (D7). The previous draft didn't list it.

## Canvas summary
Settlement collects the money for each priced ride: when Pricing reports a Ride Priced, Settlement charges the rider through Payments, retries failed charges, and after the 3rd failed Charge Attempt records Outstanding Debt, which it hands to DebtRecovery as Debt Recorded. Strategic classification and domain roles aren't in the canvas yet (Execution Model, "performs or tracks a job", looks like the fitting trait from `docs/model-traits-worksheet.md`).

## Decisions
| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | ACL towards Payments | **no ACL, conformist** (user insisted after the pushback, confirmed by approving the plan) | The reference map also draws Settlement as `[D,CF]` of Payments (OHS/PL). Risk, recorded on purpose: every change to Payments' published language (renamed fields, new charge statuses, new error codes) ripples straight into Settlement's domain code and tests, and Payments' terms ("charge", "payment method", their status codes) mix with ours ("Charge Attempt", "Outstanding Debt"). Written into `infra/adapters/secondary/paymentsctx/package-info.java`. |
| D2 | Modules | 1 module = 1 context | default |
| D3 | Package name | `settlement` | from the canvas name; matches CML `BoundedContext Settlement` |
| D4 | Aggregates | **`Settlement`**, one per priced ride, identified by `RideId` | Both confirmed rules have Givens about one ride's Charge Attempts ("a priced ride of 12.50 PLN with N failed charge attempts"). That count and the ride's price are all the state the rules need, so the smallest consistency boundary is one ride's Settlement. |
| D5 | Inbound Ride Priced | our own `RidePriced` record + `SettleRideUseCase`; no Pricing listener yet | Pricing doesn't exist in code, so there is no upstream event type to listen to. The listener is a follow-up. |
| D6 | Money type | `org.joda.money.Money` | already on the classpath and used by CrowdSorcery; the canvas talks about amounts |
| D7 | How Charge Attempt outcomes enter Settlement | `RecordChargeAttemptOutcomeUseCase` + `ChargeAttemptOutcome(rideId, succeeded)` record | Q5 is still open. Both answers (a synchronous Charge Rider result, or Payments events) end up calling this use case, so it doesn't pre-empt the answer. `RiderCharger.chargeRider` stays `void` until Q5 is answered. |
| D8 | Outcomes reported after the Settlement finished | `IllegalStateException`, no test | Not a confirmed rule (Q6). The aggregate has to do something; rejecting is the least surprising guard, and the code comment points here. Not listed as a business decision in the javadoc. |

**Pushback on D1 (given once, in the previous round).** Conformist means we take Payments' model as-is, so their model becomes part of ours. A thin ACL (a `RiderCharger` port in Settlement's words plus a translating adapter in `infra/adapters/secondary/paymentsctxacl`) costs one interface and one class. You approved the plan with "no ACL", so it's built that way.

Note on "use their API directly": Payments doesn't exist in code yet, so there is no Payments API to call. The adapter is a stub (`UnsupportedOperationException("implement this")`) and Payments' types aren't invented. With the conformist variant, the port will switch to Payments' published types once they exist.

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Pricing | Event | Ride Priced | `api/usecases/RidePriced` (record: rideId, riderId, price) + `api/usecases/SettleRideUseCase` (`@PrimaryPort`): creates the `Settlement`, saves it, makes Charge Attempt 1 through `RiderCharger`. Listener in `infra/adapters/primary/pricingctxacl` deferred: Pricing isn't in code. |
| (Payments, see Q5) | n/a | Charge Attempt outcome (not in the canvas) | `api/usecases/ChargeAttemptOutcome` + `api/usecases/RecordChargeAttemptOutcomeUseCase` (D7) |

### Outbound
| Message | Type | Collaborator | Port | Adapter (package) |
|---|---|---|---|---|
| Charge Rider | Command | Payments | `domain/charging/RiderCharger` (`@SecondaryPort`, `chargeRider(RideId, RiderId, Money)`) | `infra/adapters/secondary/paymentsctx/PaymentsRiderCharger` (`@SecondaryAdapter`, stub). Package **without** `@AntiCorruptionLayer`; its `package-info.java` records the conformist decision and its risk (D1). |
| Debt Recorded | Event | DebtRecovery | n/a | Domain event `SettlementEvent.DebtRecorded` → `infra/adapters/secondary/IntegrationEventsPublisher` → `api/events/DebtRecorded` (record, `IntegrationEvent`: rideId, riderId, amount, currency) |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Settlement | `domain/settlement/Settlement` | aggregate root |
| Charge Attempt | `Settlement.ChargeAttempt` (private record: number, outcome) | entity (immutable, record) |
| Outstanding Debt | `domain/settlement/OutstandingDebt` (record, `Money amount`) | VO |
| (ride id, rider id: from the messages) | `domain/charging/RideId`, `domain/charging/RiderId` (records) | VO |
| (facts) | `SettlementEvent.ChargeAttemptFailed`, `RideSettled`, `DebtRecorded` | domain events |

### Business rules → aggregates
| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| Given a priced ride of 12.50 PLN with 2 failed charge attempts, when the 3rd charge attempt fails, then an Outstanding Debt of 12.50 PLN is recorded and Debt Recorded is published | At most 3 Charge Attempts; the 3rd failure records Outstanding Debt equal to the ride price, once | `Settlement` (`MAX_CHARGE_ATTEMPTS = 3`) | `SettlementTest#thirdFailedChargeAttemptRecordsOutstandingDebt` |
| Given a priced ride of 12.50 PLN with 1 failed charge attempt, when the 2nd charge attempt succeeds, then the ride is settled and no Outstanding Debt is recorded | A successful Charge Attempt settles the ride and records no debt | `Settlement` | `SettlementTest#successfulSecondChargeAttemptSettlesRide` |

"Debt Recorded is published": the aggregate returns the `DebtRecorded` domain event, `RecordChargeAttemptOutcomeUseCase` hands it to `DomainEventPublisher`, and `IntegrationEventsPublisher` turns it into `api.events.DebtRecorded`. The unit test checks the domain event. The Spring path is covered only by the application-context test starting, not by a dedicated test.

### Rules awaiting GWT
- **When retries are made**, as written: "we retry a few times shortly after the ride". Status: awaiting confirmation. Not modelled: nothing triggers Charge Attempts 2 and 3 yet. Suggested strict versions (values made up, please correct):
  - *Given* Charge Attempt 1 for ride R-1 failed at 10:00, *when* it is 10:05, *then* Charge Attempt 2 of 12.50 PLN is sent.
  - *Given* Charge Attempts 1 and 2 for ride R-1 failed (10:00, 10:05), *when* it is 10:35, *then* Charge Attempt 3 of 12.50 PLN is sent.
  - **Q2** Is the clock measured from the ride end or from the first attempt?
  - Once confirmed: a `RetryDueChargesUseCase` called by a scheduled primary adapter, with a `Clock` injected.
- **Q4** Does a ride priced at 0 (for example a free period) create a Settlement and a charge at all? Suggested: *given* a ride priced at 0.00 PLN, *when* Ride Priced is received, *then* the ride is settled without a Charge Attempt.
- **Q5 (canvas gap)** How does Settlement learn that a Charge Attempt failed or succeeded? Either Charge Rider is synchronous and returns the result (OHS call), or Payments publishes events such as Charge Succeeded / Charge Failed (a new inbound row in the canvas). D7 works for both.
- **Q6 (new)** What happens to an outcome reported after the Settlement finished (for example a late success after the debt was recorded)? Suggested: *given* ride R-1 with Outstanding Debt of 12.50 PLN recorded, *when* a Charge Attempt for R-1 succeeds, then ... (the debt is cleared? or the charge is refunded?). This may belong to DebtRecovery. Today the aggregate throws `IllegalStateException` (D8).
- **Q3 (partly answered)**: the confirmed examples record the full price as debt. If attempts can ever partially succeed, a new example is needed.

No internal contradictions found among the definitions, the purpose and the two confirmed examples. One wording point: the purpose says "every ride *eventually* gets paid", but Settlement itself stops at recording debt. That's consistent if "eventually" includes DebtRecovery's work; please confirm.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Inbound **Ride Priced event from Pricing** | Rentals sends **Rental Session Ended** to Settlement; Settlement then **asks Pricing** for the price (Pricing OHS, called by Settlement) | Yes: "Pricing reacting to Rental Session Ended instead of being called by Settlement". Then Settlement doesn't need Rental Session Ended. |
| Outbound event named **Debt Recorded** | **Rental Fee Unpaid** (to DebtRecovery and Accounts) | No. Same fact, different name; please pick one. |
| No outbound to **Accounts** | Settlement → Accounts: Rental Fee Unpaid (block), Outstanding Debt Cleared (unblock) | No. Who blocks a rider with debt? |
| No inbound from **DebtRecovery** | DebtRecovery asks Settlement to collect ("attempt collection", Settlement OHS) | No. Is collection during recovery still Settlement's job? |
| No **rider's balance** | Settlement owns the rider's balance | No |
| Settlement conformist to Payments | `Payments [U,OHS,PL]->[D,CF] Settlement` | Matches the reference (conformist there too) |
| Term "ride" | "rental session" / "finished ride" | Terms used interchangeably in the map; fine |

Built from the canvas, as the skill requires. The reference map is not changed.

## Files
```
docs/bounded-contexts/velocity/settlement/
  canvas.md                                           Business Decisions: the two confirmed GWT examples added
  plan.md                                             this file
src/main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/
  package-info.java                                   @BoundedContext(name = "Settlement") + @Module, canvas javadoc
  api/usecases/RidePriced.java                        record (our language for Pricing's Ride Priced)
  api/usecases/SettleRideUseCase.java                 @PrimaryPort
  api/usecases/ChargeAttemptOutcome.java              record (D7)
  api/usecases/RecordChargeAttemptOutcomeUseCase.java @PrimaryPort (D7)
  api/events/DebtRecorded.java                        record implements IntegrationEvent
  domain/charging/RideId.java                         @ValueObject record
  domain/charging/RiderId.java                        @ValueObject record
  domain/charging/RiderCharger.java                   @SecondaryPort (Charge Rider)
  domain/settlement/Settlement.java                   @AggregateRoot, ChargeAttempt record inside
  domain/settlement/OutstandingDebt.java              @ValueObject record
  domain/settlement/SettlementEvent.java              sealed: ChargeAttemptFailed, RideSettled, DebtRecorded
  domain/settlement/SettlementRepository.java         @Repository @SecondaryPort
  domain/settlement/SettlementNotFound.java           exception
  infra/adapters/secondary/InMemorySettlementRepository.java      @SecondaryAdapter
  infra/adapters/secondary/IntegrationEventsPublisher.java        @SecondaryAdapter, domain → api.events
  infra/adapters/secondary/paymentsctx/package-info.java          conformist decision + risk (no @AntiCorruptionLayer)
  infra/adapters/secondary/paymentsctx/PaymentsRiderCharger.java  @SecondaryAdapter, stub
  infra/config/SettlementContextConfig.java           @Configuration: settlementRiderCharger, settlementSettlementRepository,
                                                      settlementSettleRideUseCase, settlementRecordChargeAttemptOutcomeUseCase,
                                                      settlementIntegrationEventsPublisher
src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/domain/settlement/
  SettlementTest.java                                 one test per confirmed GWT
```

## Stubs and follow-ups
- `PaymentsRiderCharger` is a stub: Payments isn't in code yet. Calling `SettleRideUseCase` at runtime throws `UnsupportedOperationException` until it exists.
- Pricing listener (`infra/adapters/primary/pricingctxacl`) deferred: Pricing isn't in code yet.
- Retry trigger (Q2) not modelled: nothing makes Charge Attempts 2 and 3 yet.
- DebtRecovery isn't in code: `api.events.DebtRecorded` is published into the void until it exists.
- CML (`src/main/cml/VeloCity-*.cml`) and `velocity/velocity-context-map.md` would need updating if you keep the differences above (Ride Priced from Pricing, Debt Recorded). Not changed unasked.
- Canvas sections still empty: strategic classification, domain roles, assumptions, verification metrics, open questions.

## Changelog
### 2026-11-01: plan drafted
- Added: plan for the Settlement scaffold; canvas saved from the inline description.
- Triggered by: request to add a Settlement context to VeloCity.

### 2026-11-01: scaffolded
- Added: the `velocity.settlement` module (files above) and `SettlementTest` with 2 GWT tests.
- Changed: canvas Business Decisions now hold the two confirmed GWT examples; plan D1 confirmed, D4 changed from "none yet" to the `Settlement` aggregate, D7 and D8 added; Q1 answered, Q3 partly answered, Q6 added.
- Triggered by: plan approval and the two strict retry rules given in the conversation.
- Verified: `./mvnw compile`, `./mvnw test -Dtest=SettlementTest` (2 tests), `./mvnw test` (7 tests, Spring context starts): all pass.
