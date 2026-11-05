The proposed map is approved and written down, and your original `src/main/cml/VeloCity-Context-Map.cml` is untouched.

**Files**
- `src/main/cml/VeloCity-Context-Map-validated.cml`: the corrected map. Validator: 10 contexts, 11 relationships, 0 errors.
- `docs/context-discovery/velocity/2026-11-05-validate-reference-map/discovery.md`: each finding mapped to the rule it answers, the four process walkthroughs, the critic log, the final validator output with how each line was resolved, and your answers.

**What the reference map breaks**
- **One indirect U/D cycle:** Accounts -> Rentals -> Settlement -> Accounts. Your "no cycles" check looked only at direct pairs. The fix is that Settlement calls Accounts' block / lift-block service, so Accounts is upstream.
- **6 unjustified Conformists:** to City, Rentals (twice), Pricing, Settlement (twice). They become ACLs.
- **2 downstreams with neither ACL nor CF:** Rentals from Accounts, and FleetMaintenance from CustomerSupport. Both get an ACL, and the CustomerSupport one is now confirmed.
- **No Customer/Supplier between internal teams:** three relationships become C/S.
- The 2 Conformists to Payments stay, justified in the CML comments.
- The contexts themselves passed the critic unchanged.

**How the review went**
- Contexts: all 8 got AGREE from the critic in round 1.
- Map: each of the 4 processes got AGREE in round 1, and the final pass got AGREE in round 2. No critic round changed a relationship; they only sharpened comments.

**Kept on purpose:** Context Mapper warns three times about an ACL inside a Customer/Supplier relationship. Our rules make C/S the internal default and an ACL the downstream default, and you want each downstream to keep its own language. The report explains this.

**Still open:**
- Deferred: billing disputes pausing Debt Recovery, and write-off or external collection after recovery is exhausted. Both are recorded as comments in the new CML.
- Not yet discussed: whether the live fee estimate during a ride is shown to riders. It wouldn't reverse any relationship.
- If you adopt the new map as the reference, `velocity/velocity-context-map.md` needs updating to match. That covers its diagram, its "No cycles" decision and its "Debt Recovery -> Accounts" alternative. I haven't changed it.
