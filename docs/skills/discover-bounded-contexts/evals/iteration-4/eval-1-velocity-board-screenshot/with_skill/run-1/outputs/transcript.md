# Transcript

## User

"Here's our VeloCity event storming board: velocity/velocity-processes.jpg. Find the bounded contexts on it. I'm the trainer, by the way, so you can't spoil anything for me."

## Facilitator -> user (round 1: reading + H1)

Since you're the trainer, nothing to hide, but your answers (not the reference solution) will decide the boundaries. Here's how I read the board; please correct me:

**Main timeline**
- *User Registration and Account Setup:* User Registered → Email Confirmed → Phone Verified → Basic Data Provided → Payment Method Added → Payment Method Verified → Agreement Signed → User Account Activated.
- *Bike Rental:* User Location Validated (and a "bike broken" sticky) → Bike Reserved → Bike Unlocked / Bike Unlock Failed / Bike Reservation Expired. After Unlocked: Rental Session Started → bike returned, or Maintenance Started → bike fixed → bike returned.
- *Pricing:* Rental Session Started → First Free Period Started/Ended → hourly billing periods with First/Second/Third Hour Fee Applied → Hourly Fee Applied → Rental Total Fee Calculated, or Max Rental Time Exceeded → Penalty Applied.

**Branches (endings of the ride)**
- *Station return:* Bike Returned To Station → Locked In Station → Fault Detected → Fault Reported, or Locking Failed → Customer Support Contacted / Bike Fault Reported.
- *Zone return:* Bike Returned To Zone → Drop Zone Accepted → Drop Zone Fee Applied, or Unauthorized Zone Detected → Unauthorized Zone Return Fee Applied.

**Disconnected clusters (no arrows in)**
- *Account Block and Debt Recovery:* Blocked Due To Invalid Card / Due To Debt → Block Notification Sent → Debt Recovery Started → Succeeded / Failed / Paused → Resumed → Succeeded / Failed.
- *Customer Support and Incident Handling:* Incident Reported → Type Detected → Assigned To Agent → Accepted (→ Escalated → Resolved, or Resolved) / Rejected / Rejected → Reopened → Resolved.

**Ambiguities:** Rental Session Started and "bike returned" each appear twice; "bike broken" feeds into Bike Reserved; Maintenance Started sits right after Unlock; no payment events at all; nothing on the board says where debt or incidents come from.

**H1, pivotal events.** Candidate phase changes: *User Account Activated*, *Bike Reserved* vs *Rental Session Started*, the two *Bike Returned* events, *Rental Total Fee Calculated*, and whatever triggers the two disconnected clusters.

Questions:
1. What's the difference between *User Account Activated* and *User Location Validated*? Does activation happen once, and validation before every rental? (Decides whether onboarding is its own phase.)
2. Which event really starts the billable phase: *Bike Reserved* or *Rental Session Started*? Does a reservation have its own rules, time limits or fees? And are the duplicated stickies (Session Started, bike returned) the same events? (Decides whether reservation is a phase of its own.)
3. Are the "Hour Fee Applied" stickies real charges to the rider, or estimates? When and where is the rider actually charged? (Decides where pricing ends and something else, such as charging, begins.)
4. What leads to *Account Blocked Due To Debt* and *Due To Invalid Card*? Where does the debt come from? (Links the debt cluster to the main timeline.)
5. Where do *Bike Fault Reported*, *Locking Failed* and "bike broken"/*Maintenance Started* lead: to incidents, to maintenance, or both? (Decides what the incident and maintenance flows are attached to.)

## Expert reply (round 1)

1. Activation happens once: the account is activated when the user has added a payment method. Validation happens before every rental, so that blocked or banned users can't rent.

2. *Rental Session Started* starts it. That's when the rider has actually rented the bike, the ride begins and price calculation can start. Reservation isn't a phase of its own. It just lets you reserve a bike before renting, and reservation, validation and unlock have no extra rules of their own. The duplicated stickies are the same events; they just appear in different flows.

3. They're only estimates, a convenience so the rider can see what the ride costs. The rider is actually charged when they return the bike or are forced to end the session. The estimate and the final charge use the same pricing rules.

4. There are two causes. *Due To Debt* means a payment for a rental wasn't settled, so that's where the debt comes from. *Due To Invalid Card* means the payment method wasn't verified on time.

5. Faults and broken bikes are probably related to incidents. When a bike is returned to a station or a zone, the rider can report it broken. The rental side checks that and publishes an event, and incident handling picks it up. The rental side doesn't care what happens after that. Maintenance runs independently of rentals; it isn't triggered by a ride.

## Facilitator -> user (round 2: confirmed pivots + H2)

Thanks, that moves a lot. What changed:

**Confirmed pivots (H1)**
- *User Account Activated*: happens once (payment method added). Before it everything is about who the user is; after it, renting. *User Location Validated* is a per-rental check, so it belongs to the rental side, not onboarding.
- *Rental Session Started*: the ride is billable from here. Reservation, validation and unlock have no rules of their own, so for now "preparation" is just the first step of renting, not a phase that needs its own owner.
- *Ride ended* (return or forced end): this is where the real charge happens. The board has no sticky for it.

**Missing events and renames I'd suggest for the board**
- add *Rental Session Ended* (with a forced-end variant)
- add *Payment Succeeded* / *Payment Failed* (or *Rental Settled* / *Unsettled*), since an unsettled payment is what creates the debt
- rename the "Hour Fee Applied" stickies to "Fee Estimated"

**H2, swimlanes.** Candidate lanes: the price estimate (ticks on its own hourly clock during the ride), debt recovery (pause/resume, long timeline), incident handling (own loops: escalate, reject, reopen; several triggers), and maintenance (independent of rides, per your answer). Station vs zone return is either two lanes or two variants of one ending.

I also see a duplication building up for H3: accounts get blocked from two places, onboarding (card not verified on time) and debt.

Questions:
1. Station return vs zone return: are they different business models with their own rules, or the same ride ending in two kinds of place (with an extra fee for an unauthorized zone)? (Decides lane vs variant.)
2. After the ride ends, who works out what the rider owes and makes sure it's paid? Does the charging itself have rules (retries, schedules), or does it just execute what it's told? (Decides whether "charging" is one responsibility or several, and where pricing ends.)
3. Does debt recovery have its own rules (which actions, on what schedule, when to pause)? And when it succeeds, does anything flow back, such as an unblock? (Decides whether debt recovery is its own context or part of charging.)
4. Who decides that a rider can't use the service: one owner whatever the reason (invalid card, debt, ban), or does each department block for its own reason? (Decides who owns blocking, which currently has two homes.)
5. When an incident is resolved, does anything flow back into rentals, payments or maintenance (a refund, a repair order, an unblock)? And does maintenance take bikes out of rental availability? (Decides how isolated the incident and maintenance lanes are.)

## Expert reply (round 2)

1. It's the same ride ending in two kinds of place. The rules are the same, except that returning the bike in an unauthorized zone costs an extra fee.

2. Settlement works out what's owed and makes sure it's paid. It has its own rules, mostly about the schedule, and they're not very complex. They're independent of the rental itself. Pricing is just a simple calculator that anyone can use. Payment doesn't know any rules; it just executes what it's told.

3. Yes. Debt recovery has its own rules about which actions to take, and its own schedule. When recovery succeeds, the account is unblocked so the rider can rent again. [improvised]

4. An account gets blocked for two reasons: an unsettled payment, or a payment method that wasn't verified on time. Each reason comes from the people who handle that problem. Either way the account ends up blocked, and the check before every rental is what stops the rider. [improvised: who decides]

5. No. A resolved incident doesn't flow back into rentals, payments or maintenance. Maintenance runs independently of rentals, but a bike that's being repaired can't be rented. [improvised: availability]

## Facilitator -> user (round 3: H3 draft)

Your answers settle the lanes. What changed:

- **Station vs zone return are variants**, not lanes: same ride, one extra fee. Both stay in the ride.
- **Estimate and final price are one responsibility** (same rules, "a calculator anyone can use"), so they form one **Pricing** candidate that changes independently of the ride.
- **The empty side of "ride ended" has an owner: Settlement.** It has its own schedule rules, independent of the rental, so it's a separate candidate even though it's simple.
- **Payment knows no rules,** so debt recovery, with its own actions and schedule, can't live inside it. **Debt Recovery** becomes its own candidate.
- **Incident Handling and Fleet Maintenance** are confirmed lanes with nothing flowing back.

**H3 draft (single responsibility):**

| Candidate | The one question |
|---|---|
| Rider Accounts | Can this rider use the service? |
| Rental | Who has which bike, and when? (validation, reservation, unlock, ride, returns, fault check) |
| Pricing | What does this cost? |
| Settlement | What does the rider owe for this rental, and is it settled? |
| Payments | Did the charge go through? |
| Debt Recovery | How do we recover what's owed? |
| Fleet Maintenance | Is this bike in working order? |
| Incident Handling | What went wrong, and is it resolved? |

**One H3 problem remains: blocking.** You said each department blocks for its own reason. Then "is this account blocked?" has two owners (onboarding and the debt side), and the pre-rental check has to ask both. H3 says a decision needs exactly one owner, so I need the business facts behind it:

Questions:
1. When a rider asks "why can't I rent?", who gives the answer? And when the debt is recovered, who lifts the block, and could that also lift a block for an unverified card? (Decides who owns the account's blocked status.)
2. Is Payments something your organisation builds and changes (its own team, its own language), or is it just the external payment processor seen from our side? (Decides whether Payments is a context or an external party.)
3. When a ride exceeds the maximum rental time, who ends the session and who decides the penalty? (Decides whether the forced end and the penalty belong to the ride or to pricing.)
4. When the rental side publishes a bike fault, does maintenance hear about it, or only incident handling? (Decides whether Fleet Maintenance is fed by rentals or fully independent.)

## Expert reply (round 3)

1. The account answers "why can't I rent?". It shows that it's blocked and gives the reason. Each reason is lifted only by its own fix: recovering the debt lifts the debt block, and verifying the card lifts the card block. Recovering a debt never lifts a card block. [improvised]

2. Payments is just the external payment processor seen from our side. We don't build it or change it, and it doesn't know any of our rules. [improvised: external processor]

3. When the maximum rental time is exceeded, the session is forcibly ended. That's when the rider gets charged, the same as on a normal return. The penalty amount comes from the same price rules as every other fee. [improvised]

4. Only incident handling hears about it. Maintenance runs independently of rentals.

## Critic verdicts (context round 1)

- Incident Handling | context round 1 | AGREE | H2 lane clear; notes: keep Bike Fault Reported in Rental, Customer Support is a channel not a context; non-blocking Q on own support rules vs helpdesk and pre-rental 'bike broken' report.
- Rider Accounts | context round 1 | AGREE | single owner of block status and reasons; others publish facts; caveat: rests on improvised answers; non-blocking Q on activation vs verification order.
- Settlement | context round 1 | AGREE | owns far side of the ride-end pivot; non-blocking Q: is Settlement's schedule about charge retries, and where exactly does it hand over to Debt Recovery.
- Pricing | context round 1 | AGREE | one question owned once; notes: Rental owns max-time limit/forced end, Pricing owns penalty tariff; caller supplies duration (stateless calculator).
- Debt Recovery | context round 1 | AGREE | own long-running lifecycle (H2) and own rules (H3); non-blocking Qs: what starts recovery, what follows final failure, does recovery settle the rental.
- Rental | context round 1 | AGREE | preparation+ride together justified by H3; notes: Rental owns max-time limit and forced end, zone authorization; move maintenance stickies off the rental lane.
- Fleet Maintenance | context round 1 | DISAGREE | nothing triggers maintenance and no owner of "needs repair" decision; availability depends on missing events (out of / back in service); expert questions on trigger, rules, "bike broken", post-repair "bike returned".

## Facilitator -> user (round 4: critic's expert questions on Fleet Maintenance)

The critics have reviewed all seven candidates. Six agree as they stand: Rider Accounts, Rental, Pricing, Settlement, Debt Recovery and Incident Handling. One disagrees: **Fleet Maintenance**.

The critic's point is that nothing on the board, and nothing you've said, starts maintenance. You said fault reports go only to incident handling and resolved incidents send nothing back. So "Is this bike in working order?" has no owner for its key decision ("this bike needs repair"). The "bike can't be rented while it's repaired" rule also depends on events the board doesn't have. I agree with that, and it needs your facts, not a guess:

1. What starts maintenance on a bike: a schedule or inspection, a technician's round, a fault the bike or dock detects, or something else? (Decides whether Fleet Maintenance owns the "needs repair" decision, and whether it needs a link to Rental or Incident Handling.)
2. Does maintenance have its own rules, such as service intervals, checklists, priorities or retiring bikes? Or does it only record "broken" and later "fixed"? (Decides whether it's a context or just a status flag on the bike.)
3. On the board, "maintenance started" comes right after "Bike Unlocked". Does a technician unlock the bike to take it away, or can a rider's ride end in maintenance? (If a ride can end in maintenance, "independent of rides" no longer holds.)
4. What does the "bike broken" sticky before Bike Reserved mean, and what does "bike returned" after "bike fixed" mean? Is it the bike going back into the rentable fleet? (Names the events the availability rule depends on, and who decides a bike is unusable.)

## Expert reply (round 4)

1. Maintenance runs on its own: scheduled inspections and technicians' rounds. Separately, a rider can report a bike broken when returning it to a station or zone. That report goes to incident handling, and if the agent finds the bike really is faulty, they ask maintenance to repair it. [improvised: inspections, rounds, agent's repair request]

2. Yes, maintenance has its own rules: service intervals, inspection checklists, repair priorities, and when to retire a bike. It's more than "broken" and then "fixed". [improvised]

3. A technician unlocks the bike to take it away. A rider's ride never ends in maintenance; the rider always returns the bike first. [improvised]

4. "Bike broken" means maintenance has taken the bike out of service, so nobody can reserve or rent it. "Bike returned" after "bike fixed" means maintenance has put the bike back into the rentable fleet. Maintenance decides whether a bike is usable. [improvised]

## Critic verdicts (context round 2)

- Fleet Maintenance | context round 2 | AGREE | trigger, rules and single owner of 'is this bike usable?' now present; confirm the Incident->Maintenance repair request with the expert.
- Incident Handling | context round 2 | AGREE | new Repair Requested has one owner; confirm against earlier 'nothing flows back' answer.

## Facilitator -> user (round 5: candidate presentation + approval + map yes/no)

All seven candidates now have the critic's agreement. Fleet Maintenance needed a second round. Its first critic found that nothing started maintenance and nobody owned "this bike needs repair". Your answers gave it triggers, rules and that decision.

**Proposed bounded contexts**

1. **Rider Accounts:** *Can this rider use the service?* Owns registration through User Account Activated, the card-verification deadline, the blocked status with its reasons (invalid card, debt), each reason's own unblock, and the block notification. Pivot: Account Activated (once). H3: the one place that answers "why can't I rent?". Others only publish facts. Critic: agree.
2. **Rental:** *Who has which bike, and when?* Owns the per-rental check (it combines account status and bike availability but owns neither), reservation, unlock, session start and end (including the forced end at max time), station and zone returns as variants, and the broken-bike check that publishes Bike Fault Reported. Pivots: Session Started and Session Ended. Preparation stays inside because it has no rules of its own. Critic: agree.
3. **Pricing:** *What does this cost?* Owns every price rule (free period, hourly fees, zone fees, max-time penalty), the running estimate and the final total. Lane: the estimate ticks on its own clock. H3: one calculator anyone can use, which changes independently. Critic: agree.
4. **Settlement:** *What does the rider owe for this rental, and is it settled?* Owns the reaction to Session Ended, the charge order to the payment processor, its schedule rules, and Rental Settled/Unsettled. It owns the far side of the ride-end pivot. Critic: agree.
5. **Debt Recovery:** *How do we recover what's owed?* Owns the recovery lifecycle (start, pause, resume, succeed, fail) with its own actions and schedule. Lane: a long timeline with its own rules. Critic: agree.
6. **Fleet Maintenance:** *Is this bike fit for service?* Owns inspections, rounds, repair priorities, retirement, taking bikes out of service and returning them. Lane: it follows the bike, not the ride. Critic: agree (round 2).
7. **Incident Handling:** *What went wrong, and is it resolved?* Owns intake, typing, assignment, escalate/reject/reopen/resolve, and the repair request when the agent confirms a fault. Lane: several triggers and its own loops. Critic: agree.

**External party:** the payment processor ("Payments"), which has no rules of ours.

**Open assumptions.** Many of your answers were improvised, and these rest on them: who blocks and unblocks, Payments being external, the forced end, and all of maintenance. One answer changed: in round 2 nothing flowed into maintenance, but in round 4 the agent asks maintenance for a repair. I'm using the later answer.

**Suggested board changes:** add Rental Session Ended (including a forced end), Payment Succeeded/Failed, Rental Settled/Unsettled, Debt Recovered, Repair Requested, Inspection Done and Bike Retired; rename "Hour Fee Applied" to "Fee Estimated", "bike broken" to "Bike Taken Out Of Service", and the post-repair "bike returned" to "Bike Returned To Service"; move the maintenance stickies to their own lane.

Questions:
1. Do you approve these seven contexts? And can you confirm that an incident agent asks maintenance for repairs (round 4), replacing the earlier "nothing flows into maintenance"?
2. Do you want a context map in Context Mapper (.cml) format?

## Expert reply (round 5)

1. Yes, I approve the seven contexts. And yes, I confirm it: when an incident agent confirms that a reported bike really is faulty, they ask maintenance to repair it. That replaces my earlier "nothing flows into maintenance". Resolved incidents still send nothing back.

2. No context map for now. Let's stop at the contexts today. We'll do canvases in the next session.

## Facilitator -> user (final message)

The seven contexts are approved, and the discovery report is saved. As you asked, there's no context map today, and the canvases come next session.

**File:** `docs/context-discovery/velocity/2026-11-05-bike-rental-board/discovery.md`. Status: `contexts approved`. It's also the resume point for next time.

**Contexts and relationships:** 7 contexts and 0 relationships, because you declined the map. The contexts are Rider Accounts, Rental, Pricing, Settlement, Debt Recovery, Fleet Maintenance and Incident Handling. The payment processor stays an external party.

**The critic round that changed something:**
- Fleet Maintenance, round 1: the critic disagreed, because nothing started maintenance and nobody decided "this bike needs repair". Your answers gave it triggers (inspections, rounds, repair requests from incident agents), rules, and ownership of whether a bike is usable. Round 2 agreed. Incident Handling was re-checked for the new repair request and agreed.

**Still open:**
- Several boundaries rest on answers you flagged as improvised: blocking and unblocking, Payments being external, the forced end, and all of maintenance.
- Non-blocking questions:
  - Is the account activated before or after the card is verified?
  - Is Settlement's schedule about retrying charges, and where does it hand over to Debt Recovery?
  - What starts debt recovery, and what happens after it finally fails?
  - Does a recovered debt settle the rental?
  - Does a failed station lock still end the session?
- Board changes are listed in the report: missing events such as Rental Session Ended, Payment Succeeded/Failed and Rental Settled/Unsettled, plus renames such as "Hour Fee Applied" to "Fee Estimated".

Next session I'll hand each context to `bounded-context-scaffolder` for its canvas. A map can follow, saved under a new name so it doesn't overwrite the reference `VeloCity-Context-Map.cml`.
