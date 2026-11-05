Yes, it breaks some of the rules. The worst problem is a cycle, and the reference doc says there are none. The CML itself is fine: it parses and renders in Context Mapper.

I didn't touch `src/main/cml/VeloCity-Context-Map.cml`. A corrected map is in a separate file, `src/main/cml/VeloCity-Context-Map-Corrected.cml`, and the full findings are in `velocity/velocity-context-map-review.md`.

## Rule violations

1. **Cycle through three contexts: Accounts -> Rentals -> Settlement -> Accounts.** I checked each arrow, and each one is a real dependency on the other context's model:
   - Rentals asks Accounts whether an account is active.
   - Settlement consumes Rentals' *Rental Session Ended*.
   - Accounts conforms to Settlement's *Rental Fee Unpaid* and *Outstanding Debt Cleared*.

   So the "No cycles" claim in `velocity/velocity-context-map.md` is wrong.
   - **Fix:** Accounts offers a generic *block / lift block (reason)* open host service, and Settlement calls it through an ACL. That makes Accounts upstream of Settlement. Settlement is still the only source of the debt block and of lifting it.
   - The invalid-card block keeps going through Payments' *Payment Method Invalidated* event. If Payments called Accounts' block service instead, Payments and Accounts would depend on each other directly.
2. **Two relationships have no patterns on either side:** Accounts -> Rentals and CustomerSupport -> FleetMaintenance. Every downstream must state ACL or CF.
3. **Conformist is used without justification.** ACL is the default, and CF is allowed only towards an established language, with a written reason.
   - **City -> Pricing** should be ACL. The City's zone map isn't a standard, and we can't influence it.
   - **These five should also be ACL,** because they are all internal VeloCity languages:
     - Rentals -> Settlement
     - Rentals -> FleetMaintenance (the clearest case: *withdrawn/available* vs. *broken/back in circulation*)
     - Pricing -> Settlement
     - Settlement -> DebtRecovery
     - Settlement -> Accounts
   - **Payments -> Accounts and Payments -> Settlement** can stay CF. The written reason: Payments' language mirrors the standard payment-provider domain.
4. **An unconfirmed relationship is marked through its name** (`FleetIssueIncident_Unconfirmed`). Mark it in a comment and name the relationship after what flows across it.
5. **Process 3 isn't fully covered.** Disputes pause debt recovery, but nothing in the CML models that. I added it as a commented, unconfirmed default: DebtRecovery is upstream and offers pause/resume through an open host service, and CustomerSupport is downstream with an ACL.

## Smaller points

- **Customer/Supplier:** all VeloCity teams are in one company, so internal U/D should be Customer/Supplier by default. Context Mapper rejects OHS and CF inside Customer/Supplier, so I only drew the two relationships without them as `[S]->[C]`. The others are plain U/D, with a comment saying they are conceptually Customer/Supplier.
- **OHS/PL:** using PL alone (for events) and OHS alone (Pricing, for an API) is fine, and each matches what actually flows.

## Checked and fine

- There is no Shared Kernel, Partnership, Big Ball of Mud or Separate Ways.
- There is no direct A<->B pair, and no context without a relationship.
- The external Payment Processor sits behind an ACL.

## Decision to tell the group

- **The service area isn't a relationship.** It is VeloCity's own operating region. Rentals owns it and validates the rider's location against it, so there should be no City -> Rentals arrow.

## Processes

I walked all four processes through the corrected map (onboarding, rent and return, settle and recover debt, fault to maintenance). Every hand-off has a relationship, and it has no cycles. The bike signalling its own faults to Rentals stays an unconfirmed default, as in the doc.

Before you show it to the group, `velocity/velocity-context-map.md` needs the same changes as the CML: the diagrams, the "No cycles" bullet, and the "Account blocks" decision.
