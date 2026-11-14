# VeloCity system

VeloCity is a city bike rental service and the current exercise of the training. The repo-root `CLAUDE.md` covers the package conventions; this file adds VeloCity-specific context.

## Sources of truth

- `docs/domains/velocity/velocity-vision.md` is the domain-expert role-play guide. It is **deliberately inconsistent and has gaps**, because participants are meant to discover and resolve them. Don't "fix" the vision. When the vision and the code disagree, ask instead of picking one.
- `docs/domains/velocity/velocity-context-map.md` is the **reference solution** for the context-mapping exercise. Its "Accepted Alternatives" section lists other valid answers. Treat it as a spoiler: don't push it on someone who is still doing the discovery exercise.
- `docs/domains/velocity/bounded-context-discovery-session.md` records how the bounded contexts were found, using three heuristics: pivotal events, swimlanes, and single responsibility.
- `src/main/cml/VeloCity-Bounded-Contexts.cml` (contexts only) and `VeloCity-Context-Map.cml` (with relationships) are the Context Mapper models of the reference map. `contextmap/` renders them to PNG. If you change a context or a relationship, keep the CML, `velocity-context-map.md`, and the code in sync.

## Artifacts of the AI-assisted flow

- `docs/bounded-contexts/velocity/<context>/canvas.md` and `plan.md`: canvas and the scaffolder's plan (with the TDD log) per context.
- `docs/bounded-contexts/velocity/domain-expert-answers.md`: the **role-played** domain-expert answers (DE-n) that filled the vision's gaps during the autonomous run of 2026-11-08. They are decisions made by Claude, not by the trainer; canvases, specs, plans and code cite them.
- `docs/bounded-contexts/velocity/integration-contracts.md`: the published languages, i.e. every REST resource and integration event between contexts.
- `docs/bounded-contexts/velocity/conversation.md`: the record of the session that built all of VeloCity.
- `docs/intents/`, `docs/specs/`, `docs/plans/`: 35 intents, all delivered.

## Bounded contexts

All eight contexts of the reference map exist in code, each with the root layout (`api`, `domain`, `infra`), an architecture test and Spring acceptance tests per intent (`<IntentCamelCase>SpecTest`). The external parties are simulated inside the context that talks to them: the payment processor in `payments/.../paymentprocessoracl`, the City zone map in `pricing/.../cityacl`, bike locks in `rentals/.../bikelocksacl`, and messaging in Accounts and Debt Recovery.

| Context | Aggregates (`domain/`) | Talks to |
|---|---|---|
| Accounts | `account.Account` (onboarding, blocks, eligibility) | Payments (verify card, REST); listens to Payments' `PaymentMethodInvalidated` |
| Payments | `paymentmethods.RiderPaymentMethods` | the simulated processor; publishes `PaymentMethodInvalidated` |
| Rentals | `bike.Bike` (availability, hold, rental session) | Accounts (eligibility, REST); publishes `RentalSessionEnded`, `BikeWithdrawn`, `BikeUnlocked`, `ReservationExpired`, `UnlockFailed` |
| Pricing | `tariff.TariffSchedule`, domain service `fee.FeeCalculator` | City zone map |
| Settlement | `ride.RideSettlement`, `debt.RiderDebt` | Pricing, Payments, Accounts (REST); listens to `RentalSessionEnded`; publishes `RentalFeeUnpaid`, `OutstandingDebtCleared` |
| DebtRecovery | `recovery.RecoveryProcess` | Settlement (collect, REST); listens to Settlement's events |
| CustomerSupport | `incident.Incident` | publishes `FleetIssueReported`; offers *Resolve Fleet Issue* |
| FleetMaintenance | `bike.Bike` (broken, maintenance) | Rentals and Customer Support (REST); listens to Rentals' and Customer Support's events |

## Integration conventions

- **Commands and queries between contexts go over REST**: a secondary port in the caller's language, implemented in `infra/adapters/secondary/<ctx>ctxacl` (`@AntiCorruptionLayer`) with `velocityRestClient` and `VelocityServiceUrls`. The adapter keeps its own request/response records and never imports the upstream's classes.
- **Integration events go over the in-process Spring bus**: the publisher maps its domain event to an `api/events` record; the consumer listens in `infra/adapters/primary/<ctx>ctxacl` with `@Async @EventListener` and calls its own use case. Listeners are asynchronous on purpose, as with a broker.
- **Time**: every context reads the shared `velocityClock`. Time-driven use cases (reservation expiry, overdue sessions, charge retries, collection attempts, uncontested rejections) are triggered by `@Scheduled` jobs, which `velocity.scheduling.enabled=false` switches off.
- **Tests**: `@VelocitySpringTest` starts the whole application with real beans on a random port, with a `MutableClock` and scheduling off. All Spring tests share one cached context, so they use their own ids. Cross-context effects are asserted with Awaitility. End-to-end journeys across contexts are in `src/test/java/.../velocity/journeys`.
- **Contract tests (local Pact, no broker)**: every relationship of the map has a consumer-driven contract in `src/test/resources/pacts` (`<Consumer>-<Provider>.json`; event publishers are message providers named `<Context>Events`). Consumer tests (`*PactTest`) sit next to the ACL adapter or listener and drive it against Pact's mock server or with the contracted message; provider tests (`<Provider>[Events]PactProviderTest`, tag `pact-provider`) verify the pacts against the running app, building provider states over REST (`common/contract/ProviderStates`). Surefire runs the providers after the consumers, so `./mvnw test` regenerates the pacts before verifying them.
- Rentals' chained commands (`bike.reserveForRide(anna).standingAt(position).given(standing).at(now)`) are the style for multi-argument commands, built from the `FluentLanguage` interfaces. `Bike` and its tests are the reference implementation for the `ddd-tactical-building-blocks` and `tdd` skills.
- `rentals/package-info.java` declares the context with `@BoundedContext(name = "Rentals")` and `@Module`; every context's `package-info.java` uses its CML `BoundedContext` name and carries the canvas as javadoc, with the business decisions naming their tests.
