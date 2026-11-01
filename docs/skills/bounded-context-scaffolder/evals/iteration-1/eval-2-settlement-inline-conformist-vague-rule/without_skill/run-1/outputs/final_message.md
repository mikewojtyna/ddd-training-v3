VeloCity now has a Settlement context at `pl.wojtyna.dddtrainingv3.velocity.settlement`. It charges a rider once their ride is priced, retries failed charges a few times, and then records the unpaid amount as Outstanding Debt. `./mvnw test` passes: 14 tests, including 9 new ones, and the Spring context loads.

**How it works**

- **Settlement** (`domain/settlement/Settlement`) is the aggregate. There is one per ride, and its id comes from the ride id, so if the same Ride Priced event arrives twice the rider is charged only once.
  - Each try is stored as a **Charge Attempt**.
  - When the last allowed attempt fails, the fee becomes **Outstanding Debt** and the settlement closes.
  - Recording an attempt is a fluent call, like Rentals: `settlement.record(chargeResult).at(instant)`. It produces one of three events: `RiderCharged`, `ChargeAttemptFailed` (with the time of the next attempt) or `DebtRecorded`.
  - It refuses an attempt on a closed settlement or before the next attempt is due.
- **Retry rule** (`ChargeRetryPolicy.standard()`): 3 attempts in total, 15 minutes apart. The last one happens 30 minutes after pricing. "A few times shortly after" was vague, so these numbers are my guess.
- **Inbound:** `RidePricedListener` receives Pricing's `RidePriced` event. `SettleRideUseCase` opens the settlement and makes the first charge straight away.
- **Retries:** `ChargeRetryScheduler` checks every minute (setting `velocity.settlement.retry-poll-interval`) and retries any settlement that is due. I added `@EnableScheduling` in `SettlementContextConfig` to make this run.
- **Outbound to Payments:** there is no ACL, as you asked. Settlement uses Payments' own `ChargeRider` command, `ChargeResult` and `ChargeRiderUseCase` directly, in both the aggregate and `AttemptChargeUseCase`.
- **Outbound to DebtRecovery:** `DebtRecordedPublisher` turns the internal debt event into `settlement.api.events.DebtRecorded` and publishes it.
- **Storage:** settlements are kept in memory only (`InMemorySettlementRepository`).
- **Tests:** `SettlementTest` covers the aggregate rules. `SettlementUseCasesTest` runs the whole retry-then-debt flow and the duplicate-event case with a fake Payments and a controllable clock.

**Changes outside Settlement**

- Pricing and Payments don't exist in code yet, so I added the smallest versions Settlement needs. Each has a `package-info.java`:
  - `velocity/pricing/api/events/RidePriced`.
  - `velocity/payments/api/usecases/{ChargeRider, ChargeResult, ChargeRiderUseCase}`.
  - A Payments bean that throws `UnsupportedOperationException`, the same kind of placeholder as `BikeStation.returnBike`.
- `velocity/CLAUDE.md` has a new Settlement section.
- I did not change the CML models or `velocity-context-map.md`.

**What I would have asked you**

1. **Ride Priced event vs. the reference map.** In the CML and `velocity-context-map.md`, Settlement *calls* Pricing (`Pricing [U,OHS]->[D,CF] Settlement : PriceCalculation`) and listens to Rentals' *Rental Session Ended*. Your brief has Pricing publishing *Ride Priced* instead, which the map lists as an accepted alternative. Should I update the CML and the map doc to match? That would also mean moving the Rentals→Settlement link to Rentals→Pricing. I left them alone because the doc is the reference solution.
2. **Retry numbers.** How many attempts, and how far apart? Should the gap be fixed or grow each time? Is "shortly after" measured from the ride's end or from when it was priced?
3. **Accounts.** The reference map has Settlement telling Accounts to block the rider when debt is recorded and to unblock them when it is cleared. Your brief lists only DebtRecovery. Is Accounts out of scope for now? Should *Outstanding Debt Cleared* exist at all?
4. **Event name.** The map calls the outbound event *Rental Fee Unpaid*. I used your *Debt Recorded*. Should the map be renamed to match?
5. **Payments' interface.** I invented Payments' API as a direct call that returns succeeded or failed. Is it actually asynchronous, with result events? And if Payments is down (an error rather than a declined card), should that count as a failed attempt? Right now the error just propagates.
6. **Rider balance.** The context description lists "rider balance", and the reference map has DebtRecovery asking Settlement to collect the debt. Should either be part of this change?
