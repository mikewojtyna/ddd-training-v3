I've built the Settlement context. Both of your retry rules are now in the code, each with a test, and all 7 tests pass. The Spring context still starts with the new configuration.

- **Plan:** `docs/bounded-contexts/velocity/settlement/plan.md` (Status: `implemented`; the review round and a changelog entry are recorded)
- **Module:** `src/main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/`
- **Tests:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/settlement/domain/settlement/SettlementTest.java`
- **Canvas:** I added your two examples to Business Decisions, next to the original sentence.

**What's in it:** 2 use cases, 1 published event, 3 ports, 1 aggregate and 2 tests.
- The `Settlement` aggregate covers one priced ride and is identified by its `RideId`. It holds the Charge Attempts, and Outstanding Debt is a value object.
- After the 3rd failed attempt, the aggregate records `DebtRecorded` with the full price. A successful attempt produces `RideSettled` and records no debt.
- `IntegrationEventsPublisher` turns the internal `DebtRecorded` event into the published `api.events.DebtRecorded`. That one carries only plain values: rideId, riderId, amount and currency.
- There are no queries.
- The javadoc in `package-info.java` lists both rules in Given/When/Then form and names the test for each.

**Decisions:**
- **No anti-corruption layer towards Payments.** You approved the plan with this choice, so the adapter is in `infra/adapters/secondary/paymentsctx` without `@AntiCorruptionLayer`. The risk is written in that package's javadoc: any change to Payments' published model goes straight into Settlement's code and tests.
- **Answered by your rules:**
  - 3 attempts in total.
  - The debt is the full ride price. Your examples have no partial charges.
- **One addition the plan didn't list:** `RecordChargeAttemptOutcomeUseCase` with a `ChargeAttemptOutcome(rideId, succeeded)` record. Nothing else can reach the aggregate while Q5 is open. It works whichever way you answer Q5.

**Still open:**
1. **When retries happen (Q2).** Your rules say how many attempts there are, but not when they're made. My suggested times (+5 min and +35 min) were not confirmed, so nothing triggers attempts 2 and 3 yet. Is the time counted from the end of the ride or from the first attempt?
2. **How Settlement learns the result of a charge (Q5).** Does Charge Rider return the result directly, or does Payments publish Charge Succeeded / Charge Failed events?
3. **Results that arrive after the Settlement is finished (Q6, new).** An example is a success that arrives after the debt was recorded. For now the aggregate rejects it with `IllegalStateException`, which isn't tested or listed as a business rule. Does it clear the debt, trigger a refund, or belong to DebtRecovery?
4. **Rides priced at 0 (Q4).** Does a free ride create a Settlement at all?

**Not in code yet:** Payments, Pricing and DebtRecovery.
- `PaymentsRiderCharger` is a stub that throws `UnsupportedOperationException`. So `SettleRideUseCase` fails at runtime until Payments exists.
- There is no listener for Pricing's Ride Priced event yet.
- Nobody receives `DebtRecorded` yet.

**Differences from the reference context map:**
- The Ride Priced event from Pricing matches one of the map's Accepted Alternatives.
- These are not covered:
  - Your event is called Debt Recorded; the map calls it Rental Fee Unpaid.
  - The map has relationships to Accounts that the canvas doesn't have: blocking and unblocking a rider who owes money.
  - The map has DebtRecovery asking Settlement to attempt collection.
  - In the map, Settlement owns the rider's balance.

`src/main/cml/VeloCity-*.cml` and `velocity/velocity-context-map.md` would need updating to match the canvas. I haven't changed them because the map may be a workshop exercise. Should I update them?
