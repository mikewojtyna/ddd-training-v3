I've saved your description as a canvas and written a draft plan for the Settlement context. I haven't generated any code yet: the plan needs your review first.

- Canvas (your inline text in the template structure, wording unchanged): `docs/bounded-contexts/velocity/settlement/canvas.md`
- Plan (`Status: draft - awaiting review`): `docs/bounded-contexts/velocity/settlement/plan.md`

**What the plan contains**
- 1 use case (`SettleRideUseCase`, fed by our own `RidePriced` record), 0 queries
- 1 published event (`DebtRecorded`)
- 1 secondary port (`RiderCharging`, which sends *Charge Rider* to Payments). Its adapter is a stub, because Payments isn't in the code yet.
- 0 ACLs in the variant you asked for
- 0 aggregates and 0 rule tests so far, because the canvas has no strict rule yet (see question 1)
- 1 architecture test (`SettlementArchitectureTest`). ArchUnit 1.5.1 gets added to `pom.xml` for it.

**Decisions and pushback**
- System `velocity`, package `settlement`, `@BoundedContext(name = "Settlement")`. The name matches the CML.
- **No ACL towards Payments: I recommend keeping one.** The reference map also marks Settlement as conformist to Payments, so your choice is a valid one. The cost: any change to Payments' published language goes straight into Settlement's domain model and tests, and Payments' terms mix with *Charge Attempt* and *Outstanding Debt*. A thin ACL is one interface in our words plus one translating class. The plan currently shows the no-ACL variant (decision D1, with the risk written down). Reply "no ACL" to confirm it, or "ACL" to switch.
- No boundary exceptions. Using only Payments' `api` already keeps to the boundary rule.

**Questions before I generate code**
1. **The retry rule isn't testable yet.** "A few times shortly after the ride" gives no attempt count, no delay and no debt amount, and I won't guess numbers into the domain model. The plan suggests four strict Given/When/Then rules: success settles the ride; a failure is retried after **15 minutes**, up to **3 attempts**; after the 3rd failure the full 12.50 PLN is recorded as Outstanding Debt and *Debt Recorded* is published; a finished Settlement takes no more attempts. Please confirm or correct the numbers. Two more things to settle: is "shortly" a delay between attempts or a total time window? And can a charge be partial? Your definition, "unpaid amount *left*", suggests it can.
2. **Payments doesn't exist in code**, so there's no Payments API to conform to yet. Should I (a) scaffold Payments' `api` first, or (b) build Settlement now with temporary `ChargeRider`/`ChargeResult` records and swap them for Payments' types later? The plan assumes (b).
3. **What should `SettleRideUseCase` do until the rule is confirmed?** The plan proposes keeping the load/execute/save/publish steps as comments only, like `RentBikeUseCase` in Rentals. The alternative, charging once and doing nothing on failure, would contradict your canvas.
4. **Classification and domain roles are empty.** My suggestion, which I'm not using until you confirm it: supporting domain, revenue, custom built, Execution Model.
5. **Differences from the reference context map:**
   - *Ride Priced* from Pricing replaces "Rentals → Rental Session Ended + Settlement calls Pricing". The map's Accepted Alternatives cover this.
   - *Debt Recorded* vs. the map's *Rental Fee Unpaid*: a naming difference, not covered.
   - The Accounts relationship (block on debt, unblock on *Outstanding Debt Cleared*) isn't in the canvas.
   - The inbound "attempt collection" from DebtRecovery isn't in the canvas.
   - "Rider's balance" isn't in the canvas.

   Are the missing pieces intentionally out of scope? The CML and `velocity-context-map.md` are unchanged. I can bring them in line once the plan is approved.

You can reply here or edit `plan.md` directly (answers, or set `Status: approved`). Running the skill again picks up from the saved plan.
