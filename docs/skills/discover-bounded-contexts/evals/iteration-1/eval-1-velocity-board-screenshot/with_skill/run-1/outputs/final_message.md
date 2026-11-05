The eight contexts are approved, and as you asked there's no context map and no canvases today. I saved the session report as the resume point for next time:

`docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md` (status: contexts approved)

**Approved bounded contexts (8)**

| Context | The one question it answers |
|---|---|
| Rider Accounts | Can this rider use the service right now? |
| Rental | Who has which bike, and when? |
| Pricing | What does this cost? |
| Settlement | What does the rider owe for this rental, and is it settled? |
| Payments | Did the charge go through? |
| Debt Recovery | How do we recover what's owed? |
| Fleet Maintenance | Is this bike in working order? |
| Incident Handling | What went wrong, and is it resolved? |

Two external parties sit outside them: the payment processor, and the city, which provides the zone map. The city part is my assumption and you haven't confirmed it.

**The answers that moved boundaries**
- *Rental Session Started* changes the phase, but preparation has no rules of its own, so preparation and the ride stay in one Rental context.
- Blocking appeared in two places. You made the account side its only owner, so the other contexts just publish the reason.
- Payments has no rules, but debt recovery does, so Debt Recovery is its own context.
- Nobody owned what happens after *Rental Session Ended*. Settlement owns it now, with its own schedule and retries.
- The estimate and the final price use the same rules, so they're one Pricing context. It changes on its own, so it stays separate from Rental.

**Critic:** 10 verdicts. The only round that changed anything was Fleet Maintenance in round 1. The critic found that no one owned the "bike out of service" decision, and nothing started maintenance. Your answers fixed it: maintenance owns the decision, it hears *Bike Fault Reported* straight from the return, and a mechanic's unlock is a separate action. Both Fleet Maintenance and Incident Handling then passed in round 2.

**Board changes I suggest** (the full list is in the report):
- Add *Rental Session Ended* (station, zone or forced), *Payment Succeeded/Failed*, *Rental Settled/Unsettled*, *Bike Taken Out Of Service / Back In Service* and *Account Unblocked*.
- Rename the hourly "Fee Applied" stickies to "Fee Estimated".
- Move the mechanic's branch into its own maintenance lane.
- Move activation to right after the payment method is added.

**Still open, for next session:**
- **Context map processes:** the six I listed (account, rental, settlement, debt, incident, maintenance). Is anything missing?
- **Team setup:** are any of the contexts bought or outsourced?
- **Payment processor API:** accept it as it is, or translate it?
- **City zone map:** confirm it comes from the city.
- **Map file:** I'll write `src/main/cml/VeloCity-Context-Map-board-discovery.cml`. The reference `VeloCity-Context-Map.cml` won't be touched.
- **Smaller questions** about event ownership and payloads. These are in the report.
- **Canvases:** after the map, through bounded-context-scaffolder.
