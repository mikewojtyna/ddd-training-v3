I added the Settlement context to VeloCity. It has the ports and adapters but no aggregate yet, because your retry rule doesn't say how many retries or when. The build passes: `./mvnw compile` and `./mvnw test` (5 tests, so the Spring context still starts).

**Files**
- Canvas: `docs/bounded-contexts/velocity/settlement/canvas.md`. You gave the canvas inline, and a file is the better input, so I saved your text in the canvas template. Sections you didn't fill in are marked TODO.
- Plan: `docs/bounded-contexts/velocity/settlement/plan.md`. It maps each canvas element to code and has a changelog. This was a non-interactive run, so I went straight from the plan to the code without waiting for your OK.
- Module: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/`
  - `package-info.java`: `@BoundedContext(name = "Settlement")`, the same name as in the CML.
  - `api/usecases/RidePriced` and `SettleRideUseCase`: when a ride is priced, the use case makes the first Charge Attempt.
  - `domain/charge/PaymentsCharging` (port), `ChargeRider`, `ChargeResult`.
  - `infra/adapters/secondary/paymentsctx/PaymentsChargingAdapter`: a stub that throws "implement this", because Payments isn't in code yet.
  - `api/events/DebtRecorded`: the event for DebtRecovery. Nothing sends it yet.
  - `infra/config/SettlementContextConfig`.
- No tests: there's no Given/When/Then rule with concrete values to test yet.

**Pushback**
1. **Skipping the ACL to Payments.** I did what you asked: Payments is called directly, and the adapter's package has no `@AntiCorruptionLayer`. I'd still suggest an ACL. Without one, every change to Payments' charging API goes straight into Settlement's port and its callers, and Payments' terms (payment method, pre-authorization) get mixed into Settlement's language. An ACL costs one translating adapter. The decision and this risk are written in the plan and in the `paymentsctx` package javadoc. Note that the reference map also has Settlement as conformist to Payments, so this choice matches it.
   - Payments doesn't exist in code, so there's no API to conform to yet. `ChargeRider` and `ChargeResult` are placeholders. Replace them with Payments' own types once Payments has them. I didn't invent Payments' API.
2. **"Retry a few times shortly after the ride, then record debt" is too vague to put in code.** Picking a number of retries or a delay myself would hide a guess in the domain model, so I didn't build the `Settlement` aggregate. The rule is listed under "Rules awaiting GWT" in the plan, with this suggested version. Please confirm or correct the values:
   - Given a Settlement for ride R-1 priced at 12.00 PLN and no attempts yet, when the first Charge Attempt fails, then a retry is scheduled for 5 minutes after the ride, and no debt is recorded.
   - Given 2 failed Charge Attempts for R-1, when the 3rd attempt fails, then Debt Recorded with Outstanding Debt 12.00 PLN, and no more attempts.
   - Given 1 failed attempt, when the 2nd attempt succeeds, then the Settlement is settled and no debt is recorded.
   - Given Debt Recorded for R-1, when another Charge Attempt is requested, then it's rejected.

   Once you confirm, I'll add the `Settlement` aggregate (one per ride, holding its Charge Attempts), these tests, and the code that sends Debt Recorded.

**Open questions**
- How many retries, and at what times after the ride? (See the rule above.)
- Can a charge be partly paid? "Outstanding Debt = unpaid amount left" suggests it can.
- Does Settlement announce a paid ride (for example "Ride Settled")? Who blocks or unblocks a rider who has debt?
- The canvas has no strategic classification and no domain roles. Execution Model from `docs/model-traits-worksheet.md` looks like the closest trait. Please confirm it.

**Not built yet because the other contexts aren't in code**
- The listener that turns Pricing's Ride Priced event into `RidePriced`. Pricing doesn't exist yet.
- The charge call in the Payments adapter. Payments doesn't exist yet.

**Differences from the reference context map** (`velocity/velocity-context-map.md`, CML)
- In the reference map, Rentals sends **Rental Session Ended** and Settlement *asks* Pricing for the price. Your canvas has Pricing sending **Ride Priced** instead. This is one of the map's "Accepted Alternatives" (Pricing reacting to the ride end).
- The reference map sends **Rental Fee Unpaid** to DebtRecovery, and DebtRecovery asks Settlement to "attempt collection". Your canvas sends **Debt Recorded** and has no inbound command from DebtRecovery. This isn't an accepted alternative.
- In the reference map, Settlement also tells Accounts to block a rider with debt (Rental Fee Unpaid) and to unblock them when the debt is paid (Outstanding Debt Cleared). Your canvas has no messages to Accounts.
- The reference map also uses "rider's balance" and "rental session" where your canvas uses "ride".

The CML files (`src/main/cml/VeloCity-*.cml`) and `velocity/velocity-context-map.md` don't match this canvas now. The VeloCity `CLAUDE.md` asks to keep them in sync with the code. I haven't changed them, because the map may be a workshop exercise. Should I update them?
