Short answer: yes, the reference map breaks several of our rules. The CML parses fine, but I wouldn't show it to the group as is. Your file is untouched. The corrections are in a separate proposal, and all critics agree with it.

**What it breaks**
1. **An indirect cycle: Accounts -> Rentals -> Settlement -> Accounts.** `velocity-context-map.md` says "No cycles". Fix, from your answer (option A): Accounts offers "place / lift a block, with this reason", and Settlement calls it.
2. **7 Conformists between your own teams.** CF is only allowed towards a well-established language. Since every team is internal and plans for its customers, these become Customer/Supplier with an ACL downstream. The City -> Pricing CF is fine.
3. **Accounts -> Rentals** has no pattern on either side.
4. **CustomerSupport -> FleetMaintenance** was unconfirmed and pointed the wrong way. The agent reports a broken bike into Maintenance's model, so FleetMaintenance is upstream. Now confirmed.
5. **Missing relationships for billing disputes:** pausing recovery, and charge correction or refund.
6. **The location check before a reservation had no owner.** Rentals owns it and needs the city's service area, so I added City -> Rentals.
7. **"Charging" was a responsibility of both Payments and Settlement (H3).** Settlement decides, Payments executes.

The proposal has 8 contexts (unchanged), 2 externals and 14 relationships, with no cycles. It validates with 0 errors. The only RULE lines are the two justified City CFs.

**Critic rounds that changed something**
- Map critic, round 1: it found the unowned location check, which led to finding 6.
- Settlement critic: it asked about refunds on already-paid rides, which now go through Charging.

**Files**
- `src/main/cml/VeloCity-Context-Map-validation.cml`
- `docs/context-discovery/velocity/2026-11-05-reference-map-validation/discovery.md`

**Still open**
- Do you approve the proposal? If you do, I'll update `VeloCity-Context-Map.cml`, `VeloCity-Bounded-Contexts.cml` and `velocity-context-map.md`. The doc's "Settlement drives block and unblock" and the "Debt Recovery -> Accounts for unblocking" alternative contradict your answers.
- What lifts an invalid-card block? I haven't asked yet, and it shouldn't move a boundary.
