# Payments bounded context: implementation plan

- **System / package**: `velocity.payments`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/payments/canvas.md`
- **Last updated**: 2026-11-08
- **Status**: implemented

## Review notes
- 2026-11-08: approved via docs/plans/rider-adds-payment-method.md, docs/plans/finance-blocks-rider-with-invalid-card.md and docs/plans/finance-charges-finished-ride.md (autonomous run, 2026-11-08). Structural decisions and the TDD-loop choice (off, D1) come from those plans.

## Canvas summary
Payments shields VeloCity from the external payment processor: it verifies cards with a PLN 1.00 pre-authorization,
keeps the rider's current card, charges it, and reports the current card's invalidation. Generic domain, compliance,
product; role: Gateway Interchange.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards the payment processor | yes: `infra/adapters/secondary/paymentprocessoracl` (outbound) and `infra/adapters/primary/paymentprocessoracl` (inbound notifications) | CML `PaymentProcessor [U,OHS,PL]->[D,ACL] Payments` |
| Boundary exceptions | none | |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | |
| TDD checkpoint loop | off (trainer's autonomy request, 2026-11-08, plans' D1) | domain still built test-first, one example per cycle, red before green |
| Aggregate style | state-based (plans' D2) | |
| Aggregate | one `RiderPaymentMethods` per rider | the invariant "at most one current card, the newest verified one" spans all the rider's cards |
| Charges | not persisted in Payments; the processor records them | charging changes no payment method; Settlement owns the ride's charge attempts |
| Integration event publishing | `IntegrationEventsPublisher` maps the domain `PaymentMethodInvalidated` to `api.events.PaymentMethodInvalidated` | scaffolder convention; only the current card's first invalidation is published (DE-73, DE-94) |
| Clock | the shared `Clock` bean (tests: `MutableClock`) injected into use cases | integration-contracts.md |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Accounts | Command | Verify Payment Method | `api/usecases/VerifyPaymentMethod` + `VerifyPaymentMethodUseCase` → `PaymentMethodVerification`; `POST /payments/payment-methods` (`PaymentsController`) |
| Settlement | Command | Charge | `api/usecases/ChargeCurrentCard` + `ChargeCurrentCardUseCase` → `ChargeResult`; `POST /payments/charges` |
| Payment Processor | Event | Card Invalidated | `api/usecases/InvalidatePaymentMethod` + `InvalidatePaymentMethodUseCase`; `POST /payments/processor-notifications/card-invalidations` (`infra/adapters/primary/paymentprocessoracl/ProcessorNotificationsController`) |
| Rider app / tests | Query | Rider's payment methods | `api/queries/PaymentMethodsQuery`, `RiderPaymentMethodsView`, `PaymentMethodsQueryHandler`, read model `PaymentMethodsReadModel` (implemented by `InMemoryRiderPaymentMethodsRepository`); `GET /payments/riders/{riderId}/payment-methods` |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Pre-authorize, Release, Charge | Command | Payment Processor (external) | `domain/paymentmethods/PaymentProcessor` | `ProcessorPaymentProcessorAdapter` over `SimulatedPaymentProcessor` (`infra/adapters/secondary/paymentprocessoracl`) |
| Payment Method Invalidated | Event | Accounts | n/a (published language) | `api/events/PaymentMethodInvalidated` via `infra/adapters/secondary/IntegrationEventsPublisher` |
| Payment Method Verified / Declined / Replaced, Former Payment Method Invalidated, Charge Succeeded / Failed | Event | none outside Payments | n/a | domain events only (`RiderPaymentMethodsEvent`) |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Payment method | `RiderPaymentMethods.PaymentMethod` (entity), `PaymentMethodId`, `PaymentMethodStatus` | domain, query view |
| Card token | `CardToken` | domain, api records |
| Pre-authorization | `PreAuthorization`, `PaymentProcessor#preAuthorize`/`release` | domain port |
| Verified / current card | `PaymentMethodStatus.VERIFIED` (only the current card is `VERIFIED`) | domain, query view |
| Invalidated | `PaymentMethodStatus.INVALIDATED`, `InvalidationReason` | domain, api event |
| Charge | `RiderPaymentMethods#charge`, `ChargeId`, `ChargeReference`, `ChargeFailureReason` | domain, api records |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| A card that passes pre-authorization is verified and becomes current (PLN 1.00 held and released) | green | `RiderPaymentMethods` (aggregate) | `RiderPaymentMethodsTest#cardPassingPreAuthorizationIsVerifiedAndBecomesCurrent` |
| A declined pre-authorization leaves the card unverified | green | `RiderPaymentMethods` | `#declinedPreAuthorizationLeavesCardUnverified` |
| A charge goes to the current card | green | `RiderPaymentMethods` | `#chargeGoesToCurrentCard` |
| A charge without a current card fails, processor not called | green | `RiderPaymentMethods` | `#chargeWithoutCurrentCardFailsWithoutCallingProcessor` |
| A declined charge fails with reason declined | green | `RiderPaymentMethods` | `#chargeDeclinedByProcessorFailsWithReasonDeclined` |
| A newly verified card replaces the current one | green | `RiderPaymentMethods` | `#newlyVerifiedCardReplacesCurrentOne` |
| A declined card doesn't replace the current one | green | `RiderPaymentMethods` | `#declinedCardDoesNotReplaceCurrentOne` |
| A card verified after a decline becomes current | green | `RiderPaymentMethods` | `#cardVerifiedAfterDeclineBecomesCurrent` |
| An invalidated (current) card is reported | green | `RiderPaymentMethods` | `#invalidatedCurrentCardIsReportedAndLeavesNoCurrentCard` |
| An old card being invalidated isn't reported | green | `RiderPaymentMethods` | `#oldCardBeingInvalidatedIsNotReported` |
| A second report for the same card isn't reported again | green | `RiderPaymentMethods` | `#secondReportForSameCardIsNotReportedAgain` |
| A report for an unknown card changes nothing (contract) | green | `RiderPaymentMethods` | `#reportForUnknownCardChangesNothing` |

## Deferred until tactical blocks are built
- Nothing left: use cases, query handler, REST adapters, repository, integration-event publisher and wiring were generated after the domain was green.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| none | | |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/
  package-info.java
  api/events/PaymentMethodInvalidated.java                    (existing published language, unchanged)
  api/usecases/VerifyPaymentMethod(.java|UseCase.java), PaymentMethodVerification.java
  api/usecases/ChargeCurrentCard(.java|UseCase.java), ChargeResult.java
  api/usecases/InvalidatePaymentMethod(.java|UseCase.java)
  api/queries/PaymentMethodsQuery.java, RiderPaymentMethodsView.java, PaymentMethodsReadModel.java, PaymentMethodsQueryHandler.java
  domain/paymentmethods/RiderPaymentMethods.java, RiderPaymentMethodsEvent.java, RiderPaymentMethodsRepository.java,
    PaymentProcessor.java, PreAuthorization.java, ProcessorAnswer.java, FluentLanguage.java,
    RiderId.java, PaymentMethodId.java, CardToken.java, ChargeId.java, ChargeReference.java,
    PaymentMethodStatus.java, InvalidationReason.java, ChargeFailureReason.java
  infra/adapters/primary/PaymentsController.java
  infra/adapters/primary/paymentprocessoracl/package-info.java, ProcessorNotificationsController.java
  infra/adapters/secondary/InMemoryRiderPaymentMethodsRepository.java, IntegrationEventsPublisher.java
  infra/adapters/secondary/paymentprocessoracl/package-info.java, SimulatedPaymentProcessor.java, ProcessorPaymentProcessorAdapter.java
  infra/config/PaymentsContextConfig.java
src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/
  PaymentsArchitectureTest.java
  PaymentsDriver.java (REST driver + recorder of published PaymentMethodInvalidated)
  RiderAddsPaymentMethodSpecTest.java, FinanceBlocksRiderWithInvalidCardSpecTest.java, FinanceChargesFinishedRideSpecTest.java
  domain/paymentmethods/RiderPaymentMethodsTest.java, FakePaymentProcessor.java
  infra/adapters/secondary/InMemoryRiderPaymentMethodsRepositoryTest.java
```

## Stubs and follow-ups
- The payment processor is simulated (`SimulatedPaymentProcessor`): token prefixes `tok_declined` (authorization
  declined) and `tok_nofunds` (capture declined), scripted answers, recorded authorizations, voids and captures. A real
  processor needs its own adapter and contract test.
- Accounts' listener for `PaymentMethodInvalidated` and its REST adapter for Verify Payment Method, Settlement's REST
  adapter for Charge: later waves.
- Open: refunds; several cards per rider (spec Q4); limit on declined attempts (spec Q5).

## Changelog
### 2026-11-08: scaffolded
- Added: everything listed under Files.
- Triggered by: intents `docs/plans/rider-adds-payment-method.md` (S1-S4), `docs/plans/finance-blocks-rider-with-invalid-card.md` (S1-S4), `docs/plans/finance-charges-finished-ride.md` (S1-S4).
- Canvas: tagged business decisions with spec IDs; added "A declined card doesn't replace the current one", "A second report for the same card isn't reported again", "A declined charge fails with reason declined" and the payment-methods query.

#### TDD log (checkpoint loop off, all implemented by Claude)
| # | Scenario | Block | Test | Red seen |
|---|---|---|---|---|
| 1 | verified card becomes current, PLN 1.00 held and released | `RiderPaymentMethods#verify` | `cardPassingPreAuthorizationIsVerifiedAndBecomesCurrent` | yes (no events) |
| 2 | declined pre-authorization leaves the card unverified | `#verify` | `declinedPreAuthorizationLeavesCardUnverified` | yes (Verified instead of Declined) |
| 3 | charge goes to the current card | `#charge` | `chargeGoesToCurrentCard` | yes (no events) |
| 4 | no current card: charge fails, processor not called | `#charge` | `chargeWithoutCurrentCardFailsWithoutCallingProcessor` | yes (exception: no current card) |
| 5 | processor declines the charge | `#charge` | `chargeDeclinedByProcessorFailsWithReasonDeclined` | yes (Succeeded instead of Failed) |
| 6 | newly verified card replaces the current one | `#verify` | `newlyVerifiedCardReplacesCurrentOne` | yes (no Replaced, old card charged) |
| 7 | declined card keeps the current one; card verified after a decline becomes current | `#verify` | `declinedCardDoesNotReplaceCurrentOne`, `cardVerifiedAfterDeclineBecomesCurrent` | no: green at once, already implied by cycles 2 and 6 (kept as specification of E3.2 and E2.2) |
| 8 | current card invalidated: reported, no current card | `#invalidate` | `invalidatedCurrentCardIsReportedAndLeavesNoCurrentCard` | yes (no events) |
| 9 | old card invalidated: recorded, not reported | `#invalidate` | `oldCardBeingInvalidatedIsNotReported` | yes (no events) |
| 10 | second report for the same card; unknown card | `#invalidate` | `secondReportForSameCardIsNotReportedAgain`, `reportForUnknownCardChangesNothing` | yes for the second report (Former… event emitted again); unknown card green at once |
| - | optimistic locking of the repository | `InMemoryRiderPaymentMethodsRepository` | `InMemoryRiderPaymentMethodsRepositoryTest` | written after the domain (persistence after domain) |
