I worked from the board, asked the domain expert two rounds of questions, and ended up with **7 bounded contexts**. The board has 7 swimlanes too, but they don't line up one to one: two lanes fold into others, one lane splits in two, and Payments has no lane of its own.

## Bounded contexts

| # | Context | What it owns | Stickies from the board | Type |
|---|---|---|---|---|
| 1 | **Rider Accounts** | Registration, verification, activation, account status (active/blocked). Answers "can this rider rent?" | User Registered ... User Account Activated; Account Blocked Due To Invalid Card | Supporting |
| 2 | **Rental** | The trip, from reservation to return. Covers reserving, unlocking, the rental session, returning at a station or a zone, a forced end, and a fault reported on return | User Location Validated, Bike Reserved, Bike Unlocked / Unlock Failed, Reservation Expired, Rental Session Started, Bike Returned To Station / to Zone, Bike Locked In Station / Locking Failed, Return To Drop Zone Accepted / Unauthorized Zone Detected, Bike Fault Reported | **Core** |
| 3 | **Pricing** | A stateless fee calculator anyone can call. Holds the free period, hourly tiers, max-time penalty, drop-zone and unauthorized-zone fees. Gives the live estimate during a ride and the final fee at the end, using the same rules | the whole Pricing lane, Drop Zone Fee Applied, Unauthorized Zone Return Fee Applied, Penalty Applied | Core/Supporting |
| 4 | **Payments** | Charges and verifies cards. Knows no business rules | User Payment Method Added/Verified; implicit in "Rental Total Fee Calculated" -> charge | Generic (likely an external gateway behind an ACL) |
| 5 | **Debt Recovery** | The recovery process and its schedule of actions against a rider who owes money. Blocking the account is one of those actions | Account Blocked Due To Debt, Debt Recovery Process Started/Paused/Resumed/Succeeded/Failed | Supporting |
| 6 | **Fleet Maintenance** | Inspections and repairs on the crew's own schedule. Also takes bikes reported as faulty | bike broken, maintenance started, bike fixed | Supporting |
| 7 | **Incident Handling (Customer Support)** | The incident lifecycle: reported or detected, typed, assigned to an agent, accepted/rejected, escalated, reopened, resolved | the whole Customer Support lane; Customer Support Contacted | Supporting |

## What I deliberately did *not* make a context
- **Reservation**: a reservation only holds a bike and has no rules of its own (expert, Q6). It's the first step of Rental.
- **Bike Station Return / Bike Zone Return**: same rules and same "Bike Returned" event. The only difference is a fee, and fees belong to Pricing. Both lanes go into Rental.
- **Notifications** ("Account Block Notification Sent"): a technical service with no business rules or team. It's shared infrastructure, not a bounded context.

## Board problems this exposes (good discussion material for the group)
1. **Maintenance is in the Bike Rental lane** ("Bike Unlocked -> maintenance started -> bike fixed -> bike returned"), and "bike broken" points into "Bike Reserved". Maintenance actually runs on its own schedule, and riders never see it. The only link is a fault a rider reports on return.
2. **"bike returned" shows up 4 times**. Each copy is the same pivotal event, and it ends the rental, together with "session force-ended". That event is the boundary between Rental and everything after it.
3. **The Pricing lane is a policy dressed up as a timeline.** "First Hour Fee Applied", "Second Hour Fee Applied" and so on are tiers of a calculation, not business events. The only real fact is the final fee/charge at return. The hourly figures during the ride are only an estimate.
4. **Payments is missing as a lane**, even though Debt Recovery starts when a payment fails.
5. **"Account Blocked" has two owners.** An invalid or unverified card is decided by Accounts. Debt is decided by Debt Recovery, which then asks Accounts to block. It's one account, checked before every rental.
6. Pivotal events I used to cut boundaries: *User Account Activated*, *Rental Session Started*, *Bike Returned / Session Force-Ended*, *Payment Failed* (missing from the board), *Account Blocked*, *Bike Fault Reported*, *Incident Reported*.

## Sketch of the relationships (upstream -> downstream)
- Rider Accounts -> Rental: account status, OHS/PL; Rental uses an ACL
- Pricing -> Rental: fee quote, OHS/PL
- Payments -> Rental: charge on return; Rental uses an ACL to the gateway
- Payments -> Rider Accounts: payment-method verification
- Payments -> Debt Recovery: `PaymentFailed` event
- Rider Accounts -> Debt Recovery: Accounts publishes a block/unblock API (OHS); Debt Recovery calls it
- Rental -> Incident Handling and Rental -> Fleet Maintenance: `BikeFaultReported` event (PL); nothing flows back

There are no cycles and no shared kernel. A fixed bike just goes back into a station, so there's no integration from Maintenance to Rental.

Parts of the expert's answers were marked as improvised: the maintenance inspection schedule, Debt Recovery owning the debt block, notifications having no team, and recovery being done in-house. Check those against the vision before you use them as the reference.

I haven't written any files. Next I can turn this into a Context Mapper `.cml` map, or into Bounded Context Canvases for the scaffolder.
