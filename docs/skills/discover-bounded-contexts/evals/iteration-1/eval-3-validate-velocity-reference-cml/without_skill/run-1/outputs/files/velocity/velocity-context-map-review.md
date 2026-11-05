# VeloCity Reference Context Map — Rule Check (2026-11-05)

Reviewed: `src/main/cml/VeloCity-Context-Map.cml`, plus `velocity/velocity-context-map.md`, which mirrors it.
Corrected proposal: `src/main/cml/VeloCity-Context-Map-Corrected.cml`. The original is unchanged.
Both files parse in Context Mapper. The corrected one renders with two intentional warnings ("ACL not necessarily needed in Customer/Supplier").

The rules come from the trainer/domain expert. They aren't written down in the repo.

## Findings

| # | Rule | Finding in original | Correction |
|---|------|---------------------|------------|
| 1 | No U/D cycles, direct or through several contexts | **Accounts -> Rentals -> Settlement -> Accounts.** Each arrow is a real model dependency: Rentals asks "is this account active?" (Accounts' model). Settlement consumes *Rental Session Ended* (Rentals' model). Accounts conforms to *Rental Fee Unpaid* / *Outstanding Debt Cleared* (Settlement's model). The doc's "No cycles" claim is false. | `Accounts [U,OHS,PL]->[D,ACL] Settlement : AccountBlocking`. Accounts speaks its own generic *block / lift block (reason)* language, and Settlement calls it. Settlement remains the only source of the debt block and its lifting. Accounts -> Rentals stays. |
| 1a | No cycles (direct pair) | Not in the original, but a trap in the fix: if Payments called Accounts' block OHS for invalid cards, Payments and Accounts would depend on each other directly (verification goes Payments -> Accounts). | Payments -> Accounts keeps its direction. Accounts reacts to Payments' *Payment Method Invalidated* and applies its own block (reason: invalid card). |
| 2 | Every downstream states ACL or CF; an upstream with no pattern is a gap | `Accounts [U]->[D] Rentals` and `CustomerSupport [U]->[D] FleetMaintenance` have no patterns on either side. | `Accounts [U,OHS]->[D,ACL] Rentals`, `CustomerSupport [S,PL]->[C,ACL] FleetMaintenance`. |
| 3 | ACL is the default. CF only towards a well-established language, with a written justification | CF without justification on City->Pricing, Rentals->Settlement, Rentals->FleetMaintenance, Pricing->Settlement, Settlement->Accounts, Settlement->DebtRecovery, Payments->Accounts, Payments->Settlement. | All become ACL except the two Payments relationships. Those keep CF, with the justification written in the CML: Payments' language (verified / invalidated, charge succeeded / failed) mirrors the standard payment-provider domain. |
| 3a | External parties: CF only to a standard model | `City [U]->[D,CF] Pricing`. The City's zone map isn't a standard, and we can't influence it. | `City [U]->[D,ACL] Pricing`. |
| 4 | Unconfirmed relationships are marked in a comment. Relationships are named after what flows | `FleetIssueIncident_Unconfirmed` puts the status in the name. | Named `FleetIssueIncident`, with a "Default assumption, NOT confirmed with the domain expert" comment. |
| 5 | The map supports all four processes | Process 3 (settle and recover debt) includes disputes pausing recovery, and nothing in the CML models it. The doc only lists it as deferred. | Added `DebtRecovery [U,OHS,PL]->[D,ACL] CustomerSupport : RecoveryPause` (pause / resume), marked unconfirmed in a comment. |
| 6 | Internal U/D relationships are Customer/Supplier by default (one company) | Only plain U/D is used. | `[S]->[C]` where Context Mapper allows it (Rentals->Settlement, CustomerSupport->FleetMaintenance). Context Mapper rejects OHS and CF inside C/S, so the other internal relationships stay plain U/D, with a comment saying they are conceptually C/S. |

## Checked and compliant

- There is no Shared Kernel, Partnership, Big Ball of Mud or Separate Ways.
- There is no direct A<->B pair. The corrected map is acyclic, in this topological order: PaymentProcessor, City, Payments, Accounts, Rentals, Pricing, Settlement, DebtRecovery, CustomerSupport, FleetMaintenance.
- No context lacks a relationship.
- OHS/PL usage matches what flows across each relationship:
  - PL alone (Rentals -> Settlement) is event publishing.
  - OHS alone (Pricing -> Settlement, Accounts -> Rentals) is a query API.
  - OHS+PL is used where there are both commands and events.
- The external Payment Processor sits behind an ACL in Payments.

## Process walk-through (corrected map)

1. **Onboarding and activation.**
   - Accounts verifies the payment method through Payments (OHS/PL, CF).
   - The other onboarding steps happen inside Accounts.
2. **Rent and return.**
   - Rentals asks Accounts whether the account is active (OHS, ACL).
   - Reserve/unlock, returns, locking and the maximum-rental-time end all happen inside Rentals.
   - Rentals validates the rider's location against the **service area, which Rentals owns. It is VeloCity's operating region, not the City's zone map, so there is no City -> Rentals arrow** (expert decision).
3. **Settle and recover debt.**
   - Settlement consumes *Rental Session Ended* and asks Pricing for the price. Pricing uses the City's zone map through its ACL.
   - Settlement charges through Payments, with retries.
   - When a fee stays unpaid, Settlement calls Accounts' block service and Debt Recovery starts its process. Debt Recovery asks Settlement to attempt collection.
   - When the debt is cleared, Settlement calls Accounts to lift the block.
   - A dispute in Customer Support pauses or resumes Debt Recovery (unconfirmed default).
   - An invalid card reaches Accounts as Payments' *Payment Method Invalidated* event, and Accounts blocks the account.
4. **Bike fault to maintenance.**
   - Rentals withdraws a bike itself when it detects a fault, and publishes the fault to Fleet Maintenance.
   - Fleet Maintenance reserves and unlocks the bike in Rentals for repair, then makes it available again.
   - A fleet-issue incident goes from Customer Support to Fleet Maintenance (unconfirmed default).
   - Faults the bike signals itself go to Rentals (unconfirmed default, as in the doc).

## Follow-up for the trainer

`velocity/velocity-context-map.md` needs the same changes before the workshop:
- both mermaid diagrams
- the legend example
- the "No cycles" and "Account blocks" bullets under Key Design Decisions
- the "Deferred / Open" entry for billing disputes, which is now an unconfirmed default on the map
- the "Debt Recovery -> Accounts for unblocking" accepted alternative, which needs a re-check: with Accounts upstream, it would become Accounts -> DebtRecovery
