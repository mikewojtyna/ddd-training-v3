Settlement now exists as a module, and its two retry rules are enforced by passing tests. `./mvnw compile` and `./mvnw test` both pass, 10 tests in total, and the Spring context still starts.

- **Plan:** `docs/bounded-contexts/velocity/settlement/plan.md` (status `implemented`). Your review and the changes it caused are under "Review notes" and "Changelog".
- **Module:** `src/main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/`
- **Tests:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/`

## Your two rules
- **R1 (debt after the 3rd failed attempt):** a priced ride of 12.50 PLN with 2 failed attempts; the 3rd fails; an Outstanding Debt of 12.50 PLN is recorded. Tested in `SettlementTest#debtRecordedWhenThirdChargeAttemptFails`. The "Debt Recorded is published" part has its own test, `IntegrationEventsPublisherTest#outstandingDebtIsPublishedAsDebtRecorded`.
- **R2 (2nd attempt succeeds):** the ride is settled and no debt is recorded. Tested in `SettlementTest#rideSettledWhenSecondChargeAttemptSucceeds`.
- R1 sets the limit at 3 attempts, so I didn't have to guess it.
- I checked both rules against the definitions in the canvas and found no contradictions.
- I added both rules to the Business Decisions section of `canvas.md`, and the javadoc in `package-info.java` lists them with their tests.

## What I built and decided
- **Aggregate `Settlement`:** one per priced ride. It holds the ride's Charge Attempts as an `@Entity` record and records Outstanding Debt as a value object wrapping `joda Money`.
- **Events:** two domain events, `RideSettled` and `OutstandingDebtRecorded`. The second one is published as `api.events.DebtRecorded` for DebtRecovery.
- **No ACL towards Payments (D1):** this is final because you approved that variant. The risk is written in `infra/adapters/secondary/paymentsctx/package-info.java`: any change to how Payments describes a charge goes straight into Settlement's model and tests, and their terms mix with ours.
- **Payments types (Q3):** Payments isn't in code yet, so `ChargeRider` and `ChargeResult` are temporary local records. They get replaced when Payments has its own `api` package.
- **Context boundary:** `SettlementArchitectureTest` checks that other contexts use only Settlement's `api` package and Settlement uses only theirs, with no exceptions. I added ArchUnit 1.5.1 to `pom.xml` (test scope) for it.

## What doesn't work end to end yet
- **No retries are triggered (D9).** `SettleRideUseCase` makes only the first charge attempt. The aggregate can handle attempts 2 and 3, but nothing calls them, because "shortly after the ride" still has no example.
- **The Payments adapter is a stub.** `PaymentsRiderChargingAdapter` throws `UnsupportedOperationException`, so `SettleRideUseCase` fails at runtime until Payments exists.
- **No listener for Pricing's Ride Priced event yet:** Pricing isn't in code.
- **Nobody receives Debt Recorded yet:** DebtRecovery isn't in code.

## Rules still awaiting a Given/When/Then example
These are in the plan under "Rules awaiting GWT":
- **S1:** the 1st attempt succeeds, so the ride is settled. The code already does this, because any success settles the ride, but there's no test until you confirm.
- **S2:** when retries happen ("shortly after the ride"). My placeholder is 15 minutes apart. It could also be a total window, or both.
- **S3:** a finished Settlement rejects further attempts. For now this is only a programming-error check (`IllegalStateException`, D8). It becomes a domain exception with a test once you confirm it.
- **S4:** can a charge be partial, leaving a debt smaller than the price? R1 records the full price.

## Other open points
- **Q4:** the canvas still has no strategic classification or domain roles. My earlier suggestions were not applied.
- **Q5:** the canvas differs from the reference context map:
  - The Accounts relationship is missing (blocking a rider who has debt).
  - The inbound "attempt collection" request from DebtRecovery is missing.
  - The canvas says "Debt Recorded" where the map says "Rental Fee Unpaid".
  - Pricing tells Settlement the price with Ride Priced. The map has Settlement asking Pricing, but its "Accepted Alternatives" section covers this.

`src/main/cml/VeloCity-*.cml` and `velocity/velocity-context-map.md` don't match this canvas yet. I left them unchanged in case the map is a workshop exercise. Should I update them?
