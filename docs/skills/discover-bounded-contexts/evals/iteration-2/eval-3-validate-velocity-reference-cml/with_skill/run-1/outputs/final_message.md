Done. Your reference map parses cleanly, but it breaks three of our context-mapping rules. Your original `src/main/cml/VeloCity-Context-Map.cml` is untouched.

**What the original breaks:**
- **An indirect U/D cycle:** Accounts → Rentals → Settlement → Accounts. That also contradicts the "No cycles" line in `velocity-context-map.md`, which you said you'll fix.
- **8 Conformists that aren't justified**, all of them between internal teams with no well-established language.
- **2 downstreams with no ACL:** Accounts → Rentals, and CustomerSupport → FleetMaintenance.
- **1 unconfirmed relationship.** It turns out to point the wrong way: Fleet Maintenance offers fault reporting, and Customer Support uses it.

**Proposed fix** (a new file; it validates with 0 errors and no rule warnings):
- `src/main/cml/VeloCity-Context-Map-validated.cml`: 8 contexts, 2 external parties, 11 relationships. Every downstream now uses an ACL.
- Report: `docs/context-discovery/velocity/2026-11-05-validate-reference-map/discovery.md`

**The changes that mattered:**
- **Cycle broken.** Accounts now offers "block / unblock with a reason", and Settlement calls it. To keep Payments from forming a new cycle with Accounts, Payments publishes "Payment Method Invalidated" and Accounts translates it into a block.
- **Payments / Settlement "Charging" overlap:** a critic agreed it's only wording, since Payments has no rules of its own. I renamed the responsibilities.
- **Final map critic:** it agreed. Its notes tidied the CML, and its service-area question confirmed that Rentals needs no link to the City.

**Still open:** billing disputes pausing Debt Recovery. You deferred this on purpose, so it's only a comment in the CML.

No canvases, as you asked.
