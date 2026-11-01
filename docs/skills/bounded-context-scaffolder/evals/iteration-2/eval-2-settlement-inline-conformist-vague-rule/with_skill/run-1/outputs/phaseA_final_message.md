I wrote a plan for the Settlement context and stopped there. I haven't generated any code, because the plan has to be reviewed first.

- **Plan:** `docs/bounded-contexts/velocity/settlement/plan.md` (status: `draft - awaiting review`)
- **Canvas:** I saved your description as `docs/bounded-contexts/velocity/settlement/canvas.md`, using the canvas template. The sections you didn't cover are marked TODO: classification, domain roles, assumptions, metrics and open questions.

**What the plan covers so far:** 1 use case (`SettleRideUseCase`, which consumes our own `RidePriced` record), 0 queries, 1 integration event (`DebtRecorded`), 1 outbound port (`RiderCharger`, for Charge Rider) with a stub adapter, 0 ACLs, 0 aggregates and 0 tests. Wiring goes in `SettlementContextConfig`. The name "Settlement" matches the CML.

**Decisions and pushback**
- **No ACL towards Payments.** I'd recommend against this once. Without an ACL, every change to Payments' published language goes straight into Settlement's domain code, and their terms get mixed with "Charge Attempt" and "Outstanding Debt". An ACL would cost one port and one translating class. I've planned it your way: a `paymentsctx/` package with no `@AntiCorruptionLayer`, and the risk written into its javadoc. That matches the reference map, which also has Settlement as conformist to Payments. **Please confirm you want to keep it that way.** Also, Payments doesn't exist in code yet, so there's no API to call. The adapter will be a stub either way.
- **No aggregate yet.** "Retry a few times shortly after the ride, then record debt" isn't precise enough to test. So for now Settlement only has ports and adapters: one charge attempt per Ride Priced, and failures aren't handled yet. The plan includes a proposed `Settlement` aggregate (one per priced ride) that I'll build once you confirm the rule.

**Questions for you**
1. **The retry rule.** I drafted strict Given/When/Then versions with made-up values. Please correct them:
   - 3 attempts in total
   - retries at +5 min and +35 min after the first attempt
   - after the 3rd failure: Debt Recorded with the full 12.50 PLN
   - a successful retry settles the ride and records no debt
   
   Related questions: should the timing count from the end of the ride or from the first attempt? Can an attempt be partially paid? (Your definition, "the unpaid amount *left*", suggests it can.) Does a ride that costs 0 get charged at all?
2. **How does Settlement find out that a charge failed?** The canvas has nothing coming back from Payments. Either Charge Rider returns a result directly (a synchronous call), or Payments publishes Charge Succeeded / Charge Failed events.
3. **Differences from the reference context map:**
   - The map has Rentals send Rental Session Ended and Settlement ask Pricing for the price. Your canvas has Pricing publish Ride Priced instead. This is covered by the "Accepted Alternatives" section.
   - Your "Debt Recorded" is called "Rental Fee Unpaid" in the map. Please pick one name.
   - These parts of the map aren't in the canvas: the messages to Accounts that block and unblock a rider over debt, Debt Recovery asking Settlement to collect, and the rider's balance. They're not covered by "Accepted Alternatives".

You can answer here or edit the plan file directly, then approve it by saying so or by setting `Status: approved`. When you invoke the skill again, it picks up from the saved plan. If you keep the differences, I can update the CML and `velocity/velocity-context-map.md` to match, but only if you ask.
