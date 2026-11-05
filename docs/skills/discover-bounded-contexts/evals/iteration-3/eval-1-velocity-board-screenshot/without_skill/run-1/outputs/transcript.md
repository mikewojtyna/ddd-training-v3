# Transcript

## User
Here's our VeloCity event storming board: velocity/velocity-processes.jpg. Find the bounded contexts on it. I'm the trainer, by the way, so you can't spoil anything for me.

## Assistant -> User (domain expert)
I've read the board. It has seven swimlanes: User Registration And Account Setup, Bike Rental, Pricing, Bike Station Return Handling, Bike Zone Return Handling, Account Block And Debt Recovery, Customer Support And Incident Handling. Before I draw boundaries I need a few answers, because some stickies look like they are in the wrong place or mean different things in different lanes:

1. **"bike broken" -> "Bike Reserved"** and **"Bike Unlocked" -> "maintenance started" -> "bike fixed" -> "bike returned"** in the Bike Rental lane. Is maintenance really part of a rental? Who decides a bike is broken, who repairs it, and does a rider ever see this flow?
2. **"bike returned"** appears in Bike Rental (twice), and as "Bike Returned To Station" / "Bike Returned to Zone". Are these the same event? What actually ends a rental: the rider returning the bike, the lock closing in a station, or the zone being accepted?
3. **Pricing** lists the hour-by-hour fees, plus Drop Zone Fee / Unauthorized Zone Return Fee / Penalty in other lanes. Who owns the price list and who changes it? Is the fee calculated during the ride, or once at the end? And who actually charges the rider's card - is that the same team that calculates the fee?
4. **Account Block And Debt Recovery**: who blocks an account (invalid card vs debt), and who unblocks it? Is debt recovery done by VeloCity people or an external collection agency? Is the "account" here the same thing as the account activated at registration?
5. **Customer Support**: "Bike Locking Failed -> Customer Support Contacted" and "Bike Fault Reported" - do bike faults become incidents handled by support agents, or do they go straight to the maintenance crew?
6. **Reservations**: is a reservation a separate thing from a rental (a different team, different rules, e.g. reservation expiry), or is it just the first step of a rental?

## Domain expert -> Assistant
1. No, maintenance isn't part of a rental. It runs on its own, separate from rentals. The one link is that a rider can report a bike as broken when returning it to a station or zone. Our maintenance crew does the repairs, and the rider never sees that flow [improvised: "maintenance crew"].

2. Yes, they're the same event. Duplicated stickies mean the same thing; they just show up in different flows. A rental ends when the rider returns the bike, at a station or in a zone, or when the session is forced to end. Station and zone returns follow the same rules, except for the extra fee when the bike is left in an unauthorized zone.

3. Pricing is a simple calculator that changes on its own schedule, and anyone who needs a price can use it. The business decides the price list [improvised]. The hourly amounts shown during the ride are only an estimate for the rider. The real charge is made once, when the bike is returned or the session is forced to end, and it uses the same rules as the estimate. Payment just charges the card and knows nothing about any rules, so it isn't the team that calculates the fee.

4. An account gets blocked for two reasons: an unsettled payment, or a payment method that wasn't verified in time. It's unblocked when the debt is paid or the payment method is verified [improvised]. Our own people do the debt recovery, not an external agency [improvised]. It's the same account that was activated when the user added a payment method. We check it before every rental, so blocked or banned users can't rent.

5. Bike faults are probably incidents. When a rider reports a fault on return, the rental side checks it and publishes an event. Incident handling picks that up, and the rental side doesn't know anything more about it. Riders report incidents, and some are also detected automatically. When an incident is resolved, nothing flows back into the other processes.

6. A reservation isn't a phase of its own. It just lets someone hold a bike before renting it. Getting a rental ready (checking the account, reserving, unlocking) has no extra rules of its own.

## Assistant -> User (domain expert)
Thanks, that settles most of it. Three follow-ups:

1. How does a bike get into maintenance? Does the maintenance crew pick up work from incidents (e.g. a fault reported on return becomes an incident and then a repair job), or do they inspect bikes on their own schedule? And when a bike is fixed, does anyone else need to know so it can be rented again?
2. Who owns the account block: the people who run registration/accounts, or the debt recovery people? In other words, does debt recovery just say "this rider owes X" and accounts decides to block, or does debt recovery block the account itself?
3. The "Account Block Notification Sent" stickies - is sending notifications (email/SMS/push) something with its own rules and team, or just a technical service anyone calls?

## Domain expert -> Assistant
1. Maintenance runs on its own schedule, separate from rentals. The crew does regular inspections, and they also look at bikes that riders reported as faulty [improvised: inspection schedule]. An incident doesn't feed back into any other flow when it's resolved. When a bike is fixed, it simply goes back into a station and can be rented again; nobody has to be told explicitly [improvised].

2. Debt recovery has its own rules about which actions to take against a rider who owes money, and on what schedule. Blocking the account is one of those actions, so debt recovery decides it [improvised: blocking as one of its actions]. A block for a payment method that wasn't verified in time is decided by the people who run accounts [improvised]. Either way it's the same account, and we check it before every rental.

3. Notifications are just a technical service that anyone calls. They have no business rules or team of their own [improvised].

## Assistant -> User (final)
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
