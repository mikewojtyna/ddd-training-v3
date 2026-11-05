You approved this list: eight bounded contexts, plus a ninth I'm not sure about. The swimlanes are a good starting point but don't match the boundaries one-to-one: some stickies sit in the wrong lane, and one context, Settlement, isn't on the board at all. The expert's answers settled most of this. Settlement came out of the expert's reaction to my final list: "So, who actually makes the settlement here?"

## Bounded contexts

| # | Context | What it owns | Stickies from the board | Pivotal events |
|---|---|---|---|---|
| 1 | **Rider Accounts (Onboarding)** | Registration steps, account activation, the deadline for verifying the payment method, account status (active or blocked). Rental checks this status before every rental. | All of "User Registration And Account Setup", plus "Account Blocked Due To Invalid Card" from the debt lane | User Account Activated, Account Blocked |
| 2 | **Rental** | The bike and the session: reservation, unlock, rental session, return at a station or drop zone, forced end of a session, the fault check on return. **It doesn't handle money.** | "Bike Rental" without the maintenance stickies; both return lanes without their fee stickies | Bike Reserved, Rental Session Started, Rental Ended / Forcibly Ended |
| 3 | **Pricing** | The fee calculator: free period, hourly fees, max-time penalty, unauthorized-zone fee. The live estimate and the final amount use the same rules, and the rules change on their own schedule. | "Pricing" lane, Drop Zone Fee Applied, Unauthorized Zone Return Fee Applied | (calculations, not process events) |
| 4 | **Settlement** | Settles an ended rental. It gets the final amount from Pricing, has Payments charge it, and records the rental as settled or unsettled. Its rules are mostly about when to settle and don't depend on the rental. | **Not on the board.** "Rental Total Fee Calculated" belongs here as the input. | Rental Settled, Rental Unsettled |
| 5 | **Payments** | Runs charges through the external processor and knows no business rules. Generic; mostly an anti-corruption layer (ACL) around the processor. | Not on the board | Charge Succeeded / Failed |
| 6 | **Debt Recovery** | Starts at an unsettled rental. Owns the recovery actions and their schedule: retry the charge, block the account, pause, resume, and lift the block once the debt is paid. | "Account Block And Debt Recovery" without the invalid-card branch | Debt Recovery Process Started / Succeeded / Failed |
| 7 | **Incident Handling** (customer support) | Incidents reported by riders or detected automatically: broken bike, accident, stolen or missing bike, payment complaint. Nothing flows back to other contexts. | "Customer Support And Incident Handling", Customer Support Contacted | Incident Reported, Incident Resolved |
| 8 | **Fleet Maintenance** | Taking broken bikes out of service, repairing them, putting them back into circulation | "bike broken", maintenance started, bike fixed, the technician's "bike returned" (all misplaced in the Bike Rental lane) | Bike Fixed / Back In Service |
| 9? | **City Network** (tentative) | The map of stations, drop zones and unauthorized zones, kept by the operations team. It changes only a few times a season, so it may just be reference data, not a context of its own. | Not on the board | Map changed |

## Why the lines fall where they do

- **Settlement is separate from Rental.** The rental is about the bike and the session, and money is handled only after it ends. Settlement has its own rules (when to settle) that don't depend on the ride. My first draft had Rental call Pricing and Payments at the end of the ride. That pulled settlement rules into Rental without anyone owning them.
- **Settlement, Pricing and Payments are three separate contexts.** Pricing is a calculator anyone can use, and its rules change on their own schedule. Payments only executes the charge and knows none of the rules. Settlement decides when to settle and what the outcome is. The board makes Pricing look like a clock that bills hour by hour, but the hourly amounts are only an estimate. The final amount is fixed when the rental ends.
- **Maintenance isn't Rental.** A repair ride has no session and no price. "Bike returned" means different things in the two places: for a rider it ends the rental, for a technician it puts the bike back in service. One word with two meanings is a boundary signal.
- **The two return lanes belong to Rental.** Station and drop-zone returns follow the same rules. Only the zone fees differ, and those go to Pricing.
- **The debt lane covers two different blocks.** "Blocked due to invalid card" is the payment-method verification deadline passing, so it belongs to Rider Accounts. "Blocked due to debt" is a recovery action Debt Recovery takes under its own rules and schedule, and the account just records it.
- **Incident Handling is a sink.** Rental publishes "Bike Fault Reported" and doesn't care what happens next. Support doesn't waive fees, end rides or unblock accounts.

## Relationships (first sketch, upstream -> downstream)

- Rider Accounts -> Rental: account status check before every rental
- Rental -> Settlement: Rental Ended / Forcibly Ended (event)
- Pricing -> Settlement and Rental: final amount and live estimate (Pricing as an open host service (OHS) with a published language (PL), since anyone can use it)
- Payments -> Settlement and Debt Recovery: charge execution (generic, ACL on the consumer side)
- Settlement -> Debt Recovery: Rental Unsettled starts a recovery
- Debt Recovery -> Rider Accounts: block and lift the block
- Rental -> Incident Handling: Bike Fault Reported
- Incident Handling -> Fleet Maintenance: an incident can lead to a repair
- Fleet Maintenance -> Rental: bike out of service / back in service
- City Network -> Rental, Pricing (if it stays a context)

There are no upstream/downstream cycles: Payments only answers calls, and it is downstream of nothing.

## Where the board and the expert disagree, and open questions

1. **Onboarding order.** The board puts Payment Method Verified before Agreement Signed and Account Activated. The expert says the agreement comes first, the account is activated when the payment method is *added*, and verification follows with a deadline (the rider can rent until it passes). The board needs fixing.
2. **Missing events:** Rental Settled / Unsettled, Charge Succeeded / Failed, Session Forcibly Ended, Verification Deadline Passed, Account Unblocked, Bike Back In Service.
3. **How technicians take a bike.** The board reuses "Bike Reserved" for a broken bike. The expert says a repair ride isn't a rental, but didn't say whether technicians reserve and unlock through Rental or through Fleet Maintenance.
4. **Is City Network its own context?** I'm leaning towards reference data.
5. **Bike availability.** Rental, Fleet Maintenance and Incident Handling (stolen or missing bikes) all affect whether a bike can be rented. Who owns "bike is available" is still open.

Several of the expert's answers were marked as improvised, mainly: technicians reserving bikes, the list of incident types, who decides each block, retries as a recovery action, and that settlement has no refunds or adjustments today. Treat those as softer than the rest.

As you asked, we're stopping at the contexts for today. I haven't drawn a context map or written any files. Next session we'll do the Bounded Context Canvases, and the open questions above are a good agenda for it.
