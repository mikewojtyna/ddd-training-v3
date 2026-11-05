Your reference map breaks one hard rule. It has an upstream/downstream cycle that the doc's "No cycles" check misses.

**What breaks a rule**
1. **3-context cycle: Accounts → Rentals → Settlement → Accounts.** Each of the three relationships is correct on its own. The doc's heuristic ("two contexts with arrows in both directions") only finds 2-node cycles, so it misses this one. Fix: flip the debt-block integration to `Accounts [U,OHS,PL]->[D,CF] Settlement : DebtBlocking`. Accounts offers block/unblock with a reason, and Settlement calls it on Rental Fee Unpaid or Outstanding Debt Cleared. Settlement is still the source of the "debt paid" fact, as the business requires.
2. **The accepted alternative "Debt Recovery → Accounts for unblocking" adds another cycle.** It also contradicts the business: the expert says "debt is paid" always comes from the side that owns the balance.
3. **Bare `[U]->[D]` relationships:** Accounts→Rentals and CustomerSupport→FleetMaintenance. Rentals only asks for account status and nothing flows back, so make it `Accounts [U,OHS]->[D,CF] Rentals`.

**What's wrong against the business facts I confirmed with the expert**
4. **The unconfirmed CustomerSupport → FleetMaintenance arrow points the wrong way.** Support escalates an accepted fleet-issue incident to the technicians. The technicians alone decide when the bike comes back, and support resolves the incident once it does. Proposed: `FleetMaintenance [U,OHS,PL]->[D,CF] CustomerSupport`.
5. **Missing relationship: billing dispute → pause Debt Recovery.** It's listed as "Deferred", but the expert confirmed it. Proposed: `CustomerSupport [U,PL]->[D,CF] DebtRecovery`.

**What's fine**
- No Shared Kernel or Partnership.
- Every ACL/CF is on a downstream end and every OHS/PL on an upstream end.
- One optional point for the debrief: Pricing conforms to the external City. An ACL would be the safer default there.

The expert wouldn't define the modelling rules, so I applied these: no shared kernel, no directed cycles, ACL/CF downstream, OHS/PL upstream, and explicit patterns on every relationship. Tell me if your rules differ.

I left `src/main/cml/VeloCity-Context-Map.cml` unchanged. The proposed map is cycle-free, but I haven't run it through Context Mapper. Run `VelocityContextMap` on it before the workshop. Files:
- `velocity/velocity-context-map-review.md`: the findings, plus the edits the .md doc needs (diagrams, the cycle definition, accepted alternatives, deferred items).
- `src/main/cml/VeloCity-Context-Map-Proposed.cml`: the corrected map.
