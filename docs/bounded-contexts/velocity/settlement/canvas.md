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
- **Next attempt due at**: the moment of the next charge attempt, 5 minutes after a failed one, while fewer than 3 attempts failed (DE-35).

## Business Decisions

### A finished ride is charged its fee (spec: finance-charges-finished-ride#E1.1)
- **Given** Anna's session S-1 ended and Pricing prices it at PLN 5.00
- **When** S-1 is settled and Payments charges Anna's card successfully
- **Then** S-1 is settled (Ride Settled)

### A free ride is settled without charging (spec: finance-charges-finished-ride#E2.1)
- **Given** Anna's session S-2 is priced at PLN 0.00
- **When** S-2 is settled
- **Then** S-2 is settled and Anna's card isn't charged (DE-37)

### A failed charge is retried 5 minutes later (spec: finance-retries-failed-charge#E2.1)
- **Given** the first charge for S-1 (PLN 5.00) failed at 10:40
- **When** failed charges are retried at 10:45
- **Then** a second charge attempt is made

### After the third failed attempt the fee becomes outstanding debt (spec: finance-retries-failed-charge#E2.2)
- **Given** two charge attempts for S-1 (PLN 5.00) failed and Anna has no outstanding debt
- **When** the third attempt fails at 10:50
- **Then** S-1's fee is unpaid (Rental Fee Unpaid), Anna's outstanding debt is PLN 5.00 and Settlement blocks Anna's account for *outstanding debt*

### A collected debt is cleared and the account unblocked
- **Given** Anna's outstanding debt is PLN 5.00
- **When** Debt Recovery asks to collect it and Payments charges PLN 5.00 successfully
- **Then** Anna's debt is PLN 0.00 (Outstanding Debt Cleared) and Settlement unblocks Anna for *outstanding debt*

### A rider pays off their debt in the app (spec: rider-pays-off-outstanding-debt#E1.1)
- **Given** Anna's outstanding debt is PLN 43.00 and she is blocked for *outstanding debt*
- **When** Anna pays her outstanding debt and Payments charges PLN 43.00 successfully
- **Then** Anna's debt is PLN 0.00 (Outstanding Debt Cleared) and Settlement unblocks Anna for *outstanding debt* (DE-36)

### A failed collection leaves the debt (spec: rider-pays-off-outstanding-debt#E2.1)
- **Given** Anna's outstanding debt is PLN 5.00
- **When** the collection charge fails
- **Then** Anna still owes PLN 5.00 and stays blocked

### R3 A failed first charge schedules the next attempt (spec: finance-charges-finished-ride#E3.1)
- **Given** Ben's session S-3 ended at 10:30 and is priced at PLN 43.00
- **When** S-3 is charged at 10:30 and Payments reports the charge declined
- **Then** S-3 isn't settled, this was attempt 1 of 3 and the next attempt is due at 10:35 (Charge Attempt Failed)

### R3 No current card fails the charge (spec: finance-charges-finished-ride#E3.2)
- **Given** Carla has no current card and her session S-4 ended at 10:30, priced at PLN 3.00
- **When** S-4 is charged at 10:30
- **Then** the charge fails with *no valid payment method*, this was attempt 1 of 3 and the next attempt is due at 10:35 (Charge Attempt Failed) (DE-71)

### R4 A ride is charged at most once (spec: finance-charges-finished-ride#E4.1)
- **Given** Anna's session S-1 is settled
- **When** Rental Session Ended for S-1 arrives a second time
- **Then** Anna's card isn't charged again and S-1 stays settled (DE-95)

### R1 A retry can settle the ride (spec: finance-retries-failed-charge#E1.1)
- **Given** Ben's session S-3 is priced at PLN 43.00 and attempt 1 failed at 10:30
- **When** failed charges are retried at 10:35 and Payments charges PLN 43.00 successfully
- **Then** S-3 is settled (Ride Settled)

### R1 No retry before it is due (spec: finance-retries-failed-charge#E1.2)
- **Given** attempt 1 for S-3 failed at 10:30
- **When** failed charges are retried at 10:34
- **Then** no attempt is made for S-3

### R1 A retry uses the rider's current card (spec: finance-retries-failed-charge#E1.3)
- **Given** Carla had no current card, attempt 1 for S-4 (PLN 3.00) failed at 10:30 and her card tok_mc_5555 was verified at 10:33
- **When** failed charges are retried at 10:35 and Payments charges PLN 3.00 successfully
- **Then** S-4 is settled (DE-5)

### R3 More unpaid fees add up, without a new block (spec: finance-retries-failed-charge#E3.1)
- **Given** Ben owes PLN 43.00 and is blocked for *outstanding debt*, and attempts 1 and 2 for S-5 (PLN 3.00) failed at 10:50 and 10:55
- **When** the third attempt fails at 11:00
- **Then** S-5's fee is unpaid, Ben owes PLN 46.00 (Rental Fee Unpaid) and Ben stays blocked; Accounts sends no second notification (DE-94)

### R1 The whole debt in one charge (spec: rider-pays-off-outstanding-debt#E1.2)
- **Given** Ben owes PLN 46.00 from S-3 (PLN 43.00) and S-5 (PLN 3.00) and is blocked for *outstanding debt*
- **When** Ben pays his outstanding debt and Payments charges PLN 46.00 successfully
- **Then** one charge of PLN 46.00 is made, Ben owes PLN 0.00 (Outstanding Debt Cleared) and Settlement unblocks him for *outstanding debt*

### R1 Rides still in their retries aren't part of the payoff (spec: rider-pays-off-outstanding-debt#E1.3)
- **Given** Ben owes PLN 43.00 from S-3 and attempt 1 for S-5 (PLN 3.00) failed at 10:50
- **When** Ben pays at 10:52 and Payments charges PLN 43.00 successfully
- **Then** only PLN 43.00 is charged, Ben owes PLN 0.00 (Outstanding Debt Cleared), Settlement unblocks him and S-5 stays in its charge retries (DE-95)

### R2 No card, no payoff (spec: rider-pays-off-outstanding-debt#E2.2)
- **Given** Carla owes PLN 9.00, is blocked for *outstanding debt* and *invalid card* and has no current card
- **When** Carla pays her outstanding debt
- **Then** the payment fails, she still owes PLN 9.00 and Settlement doesn't unblock her (DE-6, DE-71)

### R3 Nothing to pay off (spec: rider-pays-off-outstanding-debt#E3.1)
- **Given** Eva owes PLN 0.00
- **When** Eva pays her outstanding debt
- **Then** the payment is rejected and her card isn't charged

## Assumptions
- 🎭 DE-35: 3 attempts, 5 minutes apart.
- 🎭 DE-36: riders can pay their debt from the app.

## Verification Metrics
- Share of rides settled on the first attempt.
- Outstanding debt per month.

## Open Questions
- Refunds after a successful billing dispute: Settlement or Payments?
