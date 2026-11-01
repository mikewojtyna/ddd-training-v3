# Settlement bounded context: implementation plan

- **System / package**: `velocity.settlement` (`pl.wojtyna.dddtrainingv3.velocity.settlement`)
- **Mode**: scaffold (the package doesn't exist yet)
- **Source canvas**: `docs/bounded-contexts/velocity/settlement/canvas.md` (saved from the inline request, 2026-11-01)
- **Last updated**: 2026-11-01
- **Status**: implemented

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-01 (review 1, user): "I reviewed the Settlement plan. Here's the strict retry rule." The user gave two strict GWT rules and approved the rest of the plan ("Otherwise the plan is approved, go ahead"). Changes made:
  - **Q1** answered in part: Rule 1 (debt after the 3rd failed attempt) and Rule 2 (successful 2nd attempt settles the ride) are now R1 and R2, modelled in the `Settlement` aggregate with tests. Rule 1 fixes the attempt limit at **3**. Both rules were added to `canvas.md` under Business Decisions. Still awaiting: the timing of retries ("shortly after the ride") and the other suggestions (see "Rules awaiting GWT").
  - **Q2** (ACL): the plan was approved with the no-ACL variant, so D1 is final: conformist, no ACL. The pushback stands in this plan and in `paymentsctx/package-info.java`.
  - **Q3**: default (b) applied: temporary `ChargeRider` / `ChargeResult` records until Payments publishes its `api`.
  - **Q4**: not answered, so classification and domain roles stay out of the code (the suggestions were not applied).
  - **Q5**: not answered; the Accounts and DebtRecovery-inbound relationships stay out of scope and remain listed as differences.
  - "Use case behaviour before Q1" no longer applies: the aggregate exists, so `SettleRideUseCase` runs the four steps for the first Charge Attempt.
  - Status set to `approved`, then `implemented` after the build passed.
- 2026-11-01: draft written from the inline request, awaiting review. The inline text was saved as `canvas.md` (template structure, wording unchanged). Three things need an answer before code is generated: Q1 (the retry rule as strict GWT), Q2 (ACL towards Payments, after the pushback below), Q3 (the shape of the Payments API we conform to). See "Questions for the reviewer".

## Canvas summary
Settlement collects the money for each priced ride by charging the rider's payment method through Payments, so that every ride is eventually paid. It reacts to *Ride Priced* from Pricing, sends *Charge Rider* to Payments, and when all charge attempts fail it records *Outstanding Debt* and publishes *Debt Recorded* to DebtRecovery. Strategic classification and domain roles aren't given in the canvas.

## Questions for the reviewer
<!-- Review 1 (2026-11-01): Q1 answered in part (R1, R2; timing still open as S2), Q2 resolved as no ACL (D1), Q3 default (b) applied, Q4 and Q5 still open. -->
1. **Q1: the retry rule is not a strict Given/When/Then yet.** "Retry a few times shortly after the ride, then record debt" doesn't say how many attempts, how far apart, or what is recorded. I won't guess these numbers into the domain model. Please confirm or correct the suggested GWT examples under "Rules awaiting GWT" (number of attempts, delay between attempts, debt amount).
2. **Q2: ACL towards Payments (pushback).** You asked to skip the ACL because Settlement is conformist to Payments. The reference map agrees that the relationship is `Payments [U,OHS,PL] -> [D,CF] Settlement`, so conformist is a legitimate choice here. The cost of going without an ACL is still real: every change to Payments' published language (renamed fields, a new failure code, a changed charge result) ripples straight into Settlement's domain model and tests, and Payments' words ("charge", "payment method", processor result codes) mix into Settlement's language next to *Charge Attempt* and *Outstanding Debt*. A thin ACL (a `RiderCharging` port in our words, one translating adapter in `paymentsctxacl`) costs one interface and one class. **Recommendation: keep the ACL.** If you still want no ACL, reply "no ACL" and the plan below already shows that variant (decision D1).
3. **Q3: Payments doesn't exist in code yet.** "Use their API directly" needs `velocity.payments.api` types, and there are none. I won't invent Payments' API. Until Payments exists, the conformist adapter can only be a stub, and the port has to use temporary types. Options: (a) scaffold Payments first (or at least its `api` for *Charge Rider*), then Settlement conforms to it; (b) scaffold Settlement now with a port that takes a plain `ChargeRider` record shaped after the canvas message, and replace it with Payments' `api` type later (a breaking change inside Settlement, by design of conformism). The plan assumes (b) unless you pick (a).
4. **Q4: classification and roles.** Domain / business model / evolution and domain roles are empty. Suggestion only, not used until you confirm: Domain *supporting* (or *core*, if collection rate is a differentiator), Business model *revenue*, Evolution *custom built*, Role *Execution Model* (performs and tracks the job of collecting money per ride).
5. **Q5: differences from the reference context map** (see the table below), in particular the missing Accounts relationship (blocking a rider with outstanding debt) and the missing inbound "attempt collection" from DebtRecovery. Are they intentionally out of scope for this canvas?

## Decisions
| # | Decision | Choice | Reason |
|---|---|---|---|
| D0 | System | `velocity` | Settlement is a VeloCity context in the canvas, the vision and the CML. |
| D0a | Name / package | `@BoundedContext(name = "Settlement")`, package `settlement` | Canvas name matches the CML `BoundedContext Settlement`. |
| D0b | Modules | 1 module = 1 context | default |
| D1 | ACL towards Payments | **No ACL (conformist, user insisted; plan approved with this variant on 2026-11-01).** Adapter in `infra/adapters/secondary/paymentsctx/` without `@AntiCorruptionLayer`; the port may use Payments' published (`api`) types. | User asked explicitly for conformist without ACL; the reference map also marks Settlement CF towards Payments. Pushback given (Q2). **Risk:** upstream changes in Payments' published language ripple straight into Settlement's domain model; Payments' terms mix into Settlement's UL. The risk is spelled out in `paymentsctx/package-info.java`. If you choose the ACL, the package becomes `paymentsctxacl` with `@AntiCorruptionLayer` and the port takes only Settlement types. |
| D2 | Boundary exceptions | none | Conformist through Payments' `api` already follows the "talk only through `api`" rule. No one asked to use another context's internals. `SettlementArchitectureTest` stays two plain rules. |
| D3 | Aggregates | `Settlement` (one per priced ride, identified by `RideId`) | R1 and R2 share the same Given (a priced ride and its Charge Attempts so far) and the same kind of When (a Charge Attempt result). See "Business rules → aggregates". |
| D6 | Money | `org.joda.money.Money` instead of an own `Money` record | The code templates say to use joda-money for amounts in a currency, and it's already on the classpath. `OutstandingDebt` wraps it. |
| D7 | Attempt limit | `Settlement.MAX_CHARGE_ATTEMPTS = 3` | Fixed by R1 ("with 2 failed ... when the 3rd fails, then debt"). Not a guess. |
| D8 | Finished Settlement receiving another attempt | `IllegalStateException` as a programming-error guard, no domain exception, no test | The business answer (suggested S3) isn't confirmed. Something has to happen in code; a guard is the least domain-committing option. Replace with a domain exception once S3 is confirmed. |
| D9 | Retry trigger | none yet | When a retry happens ("shortly after the ride") is still awaiting GWT. `SettleRideUseCase` makes only the first attempt; nothing calls the aggregate for attempts 2 and 3 in the running app yet. |
| D4 | Inbound *Ride Priced* | our own record + use case; no listener yet | Pricing doesn't exist in code, so there's no upstream type to listen to. Listener `infra/adapters/primary/pricingctxacl/PricingEventsListener` is a follow-up. |
| D5 | ArchUnit | add `com.tngtech.archunit:archunit-junit5:1.5.1` (test scope) to `pom.xml` | Not on the classpath yet; older versions can't read JDK 27 class files. |

## Mapping: canvas → code
Paths relative to `src/main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/`.

### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Pricing | Event | Ride Priced | `api/usecases/RidePriced` (our record: `rideId`, `riderId`, `amount`, `currency`) and `api/usecases/SettleRideUseCase` (`@PrimaryPort`): creates the `Settlement`, sends *Charge Rider* through `RiderCharging`, records the result (`chargeAttemptSucceeded` / `chargeAttemptFailed`), saves, publishes the events. Listener on Pricing's `api.events.RidePriced`: follow-up (Pricing not in code). |

(The earlier "Use case behaviour before Q1" options are obsolete now that R1 and R2 exist.)

### Outbound
| Message | Type | Collaborator | Port | Adapter (package) |
|---|---|---|---|---|
| Charge Rider | Command | Payments | `domain/settlement/RiderCharging` (`@SecondaryPort`), method `ChargeResult charge(ChargeRider command)`; under conformism `ChargeRider`/`ChargeResult` are meant to be Payments' `api` types (temporary local records until Payments exists, Q3) | `infra/adapters/secondary/paymentsctx/PaymentsRiderChargingAdapter` (`@SecondaryAdapter`), **stub** throwing `UnsupportedOperationException("implement this")`, comment quoting the canvas message "Charge Rider → Payments". `package-info.java` javadoc records D1 and its risk. |
| Debt Recorded | Event | DebtRecovery | n/a | `api/events/DebtRecorded` (record, primitives/strings only: `rideId`, `riderId`, `amount`, `currency`) implementing `IntegrationEvent`. Emitted from the domain event `SettlementEvent.OutstandingDebtRecorded` by `infra/adapters/secondary/IntegrationEventsPublisher` (R1). |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Settlement | `domain/settlement/Settlement` | aggregate root (one per priced ride) |
| Charge Attempt | `domain/settlement/ChargeAttempt` | entity inside Settlement (`@Entity`, `@Identity number`); immutable after it's made, so a record |
| Outstanding Debt | `domain/settlement/OutstandingDebt` | value object record wrapping joda `Money` (D6) |
| (ride, rider identifiers used by messages) | `RideId`, `RiderId` records in `domain/settlement` | value objects |
| (ride settled, debt recorded) | `SettlementEvent.RideSettled`, `SettlementEvent.OutstandingDebtRecorded` | domain events (sealed hierarchy) |

### Business rules → aggregates
**Aggregate reasoning:** both rules have the same Given (the charge attempts made so far for one priced ride) and the same When (a charge attempt result). So the consistency boundary is one Settlement per ride: it holds its Charge Attempts, decides whether another attempt is due, and records Outstanding Debt when the limit is reached. Nothing else needs to be loaded.

| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| **R1** (user's Rule 1): Given a priced ride of 12.50 PLN with 2 failed charge attempts, When the 3rd charge attempt fails, Then an Outstanding Debt of 12.50 PLN is recorded and Debt Recorded is published. | the 3rd failed Charge Attempt records Outstanding Debt equal to the ride price; it's published as *Debt Recorded* | `Settlement` (+ `IntegrationEventsPublisher` for the publishing) | `SettlementTest#debtRecordedWhenThirdChargeAttemptFails`, `IntegrationEventsPublisherTest#outstandingDebtIsPublishedAsDebtRecorded` |
| **R2** (user's Rule 2): Given a priced ride of 12.50 PLN with 1 failed charge attempt, When the 2nd charge attempt succeeds, Then the ride is settled and no Outstanding Debt is recorded. | a successful Charge Attempt within the limit settles the ride; failed attempts below the limit record no debt | `Settlement` | `SettlementTest#rideSettledWhenSecondChargeAttemptSucceeds` |

Consistency check: both rules agree with the UL ("Outstanding Debt: the unpaid amount left after all attempts fail" → 12.50 PLN unpaid of a 12.50 PLN ride after all 3 attempts). No contradictions found.

### Rules awaiting GWT
The number of attempts is settled by R1 (3). The earlier suggestions that the review didn't confirm remain open (the numbers are placeholders):
- **S1 First charge succeeds.** Given a priced ride of 12.50 PLN with no charge attempts, when the 1st charge attempt succeeds, then the ride is settled and no Outstanding Debt is recorded. Status: awaiting confirmation. The code generalises R2 (any successful attempt within the limit settles), but there is no test for it until you confirm.
- **S2 When a failed charge is retried ("shortly after the ride").** Given a priced ride whose 1st charge attempt failed at 10:00, when the failure is recorded, then the 2nd charge attempt is scheduled for 10:15 (**15 minutes** apart). Status: awaiting confirmation. Is it a fixed delay, a total window (e.g. "within 1 hour of Ride Priced"), or both? Until answered, nothing triggers attempts 2 and 3 (D9).
- **S3 A finished Settlement takes no more attempts.** Given a settled ride (or one with Outstanding Debt recorded), when another charge attempt is made, then it is rejected. Status: awaiting confirmation. Guarded as a programming error for now (D8).
- **S4 Partial charges.** Can a charge take less than the price, leaving Outstanding Debt smaller than the price? The UL says "unpaid amount left", which hints it might. Status: open question. R1 records the full price.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Inbound *Ride Priced* event from Pricing (Pricing pushes the price) | Rentals publishes *Rental Session Ended* to Settlement, and Settlement *calls* Pricing ("price for session", `Pricing [U,OHS] -> [D,CF] Settlement`) | Yes: "Pricing reacting to *Rental Session Ended* instead of being called by Settlement". Consequence: Settlement no longer depends on Rentals directly; the CML relationship `Rentals -> Settlement : RentalSessionEnded` and `Pricing -> Settlement : PriceCalculation` would change to an event-based Pricing → Settlement relationship. |
| Outbound event *Debt Recorded* to DebtRecovery | *Rental Fee Unpaid* to DebtRecovery (`Settlement [U,OHS,PL] -> [D,CF] DebtRecovery`) | No, it's a naming difference ("Debt Recorded" vs. "Rental Fee Unpaid"). Building the canvas name. |
| No relationship to Accounts | *Rental Fee Unpaid* → Accounts (block) and *Outstanding Debt Cleared* → Accounts (unblock), `Settlement [U,PL] -> [D,CF] Accounts : DebtBlocking`; key decision "Settlement drives both block and unblock for debt" | No. Missing in the canvas (Q5). |
| No inbound from DebtRecovery | DebtRecovery asks Settlement to "attempt collection" (OHS) | No. Missing in the canvas (Q5). |
| No "rider's balance" | Settlement owns the rider's balance | No. Not in the canvas UL. |
| Conformist to Payments, *Charge Rider* | `Payments [U,OHS,PL] -> [D,CF] Settlement : Charging` | Matches the map (CF). |
| UL: Settlement, Charge Attempt, Outstanding Debt | Key language: charge, settled, outstanding debt | Compatible; "Charge Attempt" refines "charge". |

The reference map is not changed.

## Files
Implemented (no ACL to Payments, D1):

```
pom.xml                                                         + archunit-junit5 1.5.1 (test)
docs/bounded-contexts/velocity/settlement/canvas.md             (saved from inline input, done)
docs/bounded-contexts/velocity/settlement/plan.md               (this file)
src/main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/
  package-info.java                                             @BoundedContext(name = "Settlement") + @Module, canvas javadoc; R1, R2 implemented, timing and S3 "not modelled yet"
  api/usecases/RidePriced.java                                  record
  api/usecases/SettleRideUseCase.java                           @PrimaryPort
  api/events/DebtRecorded.java                                  record implements IntegrationEvent
  domain/settlement/Settlement.java                             @AggregateRoot
  domain/settlement/ChargeAttempt.java                          @Entity record
  domain/settlement/OutstandingDebt.java                        @ValueObject record
  domain/settlement/RideId.java, RiderId.java                   @ValueObject records
  domain/settlement/SettlementEvent.java                        sealed domain events: RideSettled, OutstandingDebtRecorded
  domain/settlement/SettlementRepository.java                   @Repository @SecondaryPort
  domain/settlement/RiderCharging.java                          @SecondaryPort
  domain/settlement/ChargeRider.java                            record (temporary stand-in for Payments' api type, Q3)
  domain/settlement/ChargeResult.java                           record (temporary stand-in for Payments' api type, Q3)
  infra/adapters/secondary/InMemorySettlementRepository.java    @SecondaryAdapter
  infra/adapters/secondary/IntegrationEventsPublisher.java      @SecondaryAdapter, OutstandingDebtRecorded → DebtRecorded
  infra/adapters/secondary/paymentsctx/package-info.java        conformist decision D1 + risk (no @AntiCorruptionLayer)
  infra/adapters/secondary/paymentsctx/PaymentsRiderChargingAdapter.java   stub
  infra/config/SettlementContextConfig.java                     @Configuration, beans prefixed "settlement"
src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/
  SettlementArchitectureTest.java                               incoming/outgoing api-only rules, no exceptions
  domain/settlement/SettlementTest.java                         R1, R2
  infra/adapters/secondary/IntegrationEventsPublisherTest.java  R1 ("Debt Recorded is published")
```

## Stubs and follow-ups
- `PaymentsRiderChargingAdapter` is a stub: Payments isn't in code yet (Q3). So `SettleRideUseCase` throws `UnsupportedOperationException` at runtime until Payments exists.
- No retry trigger (D9): attempts 2 and 3 are modelled in the aggregate but nothing calls them until S2 (timing) is confirmed.
- Listener for Pricing's *Ride Priced*: follow-up, Pricing isn't in code yet.
- DebtRecovery consumer of *Debt Recorded*: DebtRecovery isn't in code yet.
- CML (`src/main/cml/VeloCity-*.cml`) and `velocity/velocity-context-map.md` don't match the canvas (see differences). Not changed; can be updated on request.
- Canvas sections still empty: classification, domain roles, assumptions, verification metrics, open questions (Q4).

## Changelog
### 2026-11-01: scaffolded
- Added: the module and tests listed under "Files"; `com.tngtech.archunit:archunit-junit5:1.5.1` (test) in `pom.xml` (D5).
- Changed: canvas Business Decisions now carry the user's two strict GWT rules (R1, R2); plan decisions D1 finalised, D3 changed from "none" to `Settlement`, D6 to D9 added.
- Verified: `./mvnw compile`, `./mvnw test -Dtest='SettlementTest,IntegrationEventsPublisherTest,SettlementArchitectureTest'` and `./mvnw test` pass (10 tests).
- Triggered by: review 1, the user's strict retry rules and approval.

### 2026-11-01: draft
- Added: canvas saved from inline input; plan drafted. No code generated yet (awaiting review).
- Triggered by: initial request to add the Settlement context.
