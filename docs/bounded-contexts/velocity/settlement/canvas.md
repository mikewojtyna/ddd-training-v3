# Bounded Context Canvas: Settlement

<!--
Written by Claude on 2026-11-08 from docs/domains/velocity/velocity-vision.md ("Settling the Ride",
"Account Blocks & Debt Recovery"), docs/domains/velocity/velocity-context-map.md ("Zoom: from a finished ride to
money") and the CML, during an autonomous run. Values from role-played domain-expert answers (DE-n).
-->

## Name
Settlement

## Purpose
Makes sure every ride is eventually settled: charges the rider for each finished ride, retries a failed charge a few
times, and turns what still can't be collected into the rider's outstanding debt, blocking the account until the
debt is cleared. It is the only context that moves money (through Payments).

## Strategic Classification
- Domain: core
- Business model: revenue
- Evolution: custom built

## Domain Roles
- Execution Model: runs the charge → retry → debt lifecycle per ride.
- Enforcer: makes Accounts block riders with outstanding debt and unblock them once it is cleared.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Rentals | Event | Rental Session Ended | Starts settling the ride (Conformist to Rentals' PL) |
| Finance (clock) | Command | Retry Failed Charges | 5 minutes after each failed attempt (DE-35) |
| Debt Recovery | Command | Collect Outstanding Debt | Customer/Supplier: shaped by Debt Recovery's needs |
| Rider app | Command | Pay Outstanding Debt | DE-36 |
| Rider app, Debt Recovery | Query | Rider Balance | Outstanding debt and unsettled rides |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Price for Session | Query | Pricing | yes (CF) |
| Charge | Command | Payments | yes (CF) |
| Block Account / Unblock Account (reason: outstanding debt) | Command | Accounts | yes (ACL to Accounts' OHS) |
| Rental Fee Unpaid (rider, ride, amount, outstanding debt) | Event | Debt Recovery | n/a (PL) |
| Outstanding Debt Cleared (rider) | Event | Debt Recovery | n/a (PL) |
| Ride Settled | Event | none outside Settlement yet | n/a |

## Ubiquitous Language
- **Ride**: a finished rental session to be settled, identified by the session.
- **Fee**: the total fee Pricing calculated for the ride.
- **Charge attempt**: one try to charge the fee to the rider's card through Payments.
- **Settled**: the fee was charged, or the fee was PLN 0.00 (DE-37).
- **Rental fee unpaid**: all 3 charge attempts failed (DE-35); the fee becomes outstanding debt.
- **Outstanding debt**: the sum the rider owes from unpaid rides.
- **Collection**: charging the whole outstanding debt at once, asked for by Debt Recovery or by the rider.
- **Ending (when asking Pricing)**: Settlement translates Rentals' end reason into Pricing's ending: *station return* → *station*, *free-floating return* → *free-floating*, *lock failure* → *lock failure*, *maximum rental time exceeded* → *maximum rental time*.
- **Outstanding debt cleared**: the outstanding debt went down to PLN 0.00.

## Business Decisions

### A finished ride is charged its fee
- **Given** Anna's session S-1 ended and Pricing prices it at PLN 5.00
- **When** S-1 is settled and Payments charges Anna's card successfully
- **Then** S-1 is settled (Ride Settled)

### A free ride is settled without charging
- **Given** Anna's session S-2 is priced at PLN 0.00
- **When** S-2 is settled
- **Then** S-2 is settled and Anna's card isn't charged (DE-37)

### A failed charge is retried 5 minutes later
- **Given** the first charge for S-1 (PLN 5.00) failed at 10:40
- **When** failed charges are retried at 10:45
- **Then** a second charge attempt is made

### After the third failed attempt the fee becomes outstanding debt
- **Given** two charge attempts for S-1 (PLN 5.00) failed and Anna has no outstanding debt
- **When** the third attempt fails at 10:50
- **Then** S-1's fee is unpaid (Rental Fee Unpaid), Anna's outstanding debt is PLN 5.00 and Settlement blocks Anna's account for *outstanding debt*

### A collected debt is cleared and the account unblocked
- **Given** Anna's outstanding debt is PLN 5.00
- **When** Debt Recovery asks to collect it and Payments charges PLN 5.00 successfully
- **Then** Anna's debt is PLN 0.00 (Outstanding Debt Cleared) and Settlement unblocks Anna for *outstanding debt*

### A rider pays off their debt in the app
- **Given** Anna's outstanding debt is PLN 43.00 and she is blocked for *outstanding debt*
- **When** Anna pays her outstanding debt and Payments charges PLN 43.00 successfully
- **Then** Anna's debt is PLN 0.00 (Outstanding Debt Cleared) and Settlement unblocks Anna for *outstanding debt* (DE-36)

### A failed collection leaves the debt
- **Given** Anna's outstanding debt is PLN 5.00
- **When** the collection charge fails
- **Then** Anna still owes PLN 5.00 and stays blocked

## Assumptions
- 🎭 DE-35: 3 attempts, 5 minutes apart.
- 🎭 DE-36: riders can pay their debt from the app.
- A ride is settled once, even if Rental Session Ended arrives twice.

## Verification Metrics
- Share of rides settled on the first attempt.
- Outstanding debt per month.

## Open Questions
- Refunds after a successful billing dispute: Settlement or Payments?
