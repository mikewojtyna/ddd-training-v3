# Bounded context discovery: VeloCity (bike rental event-storming board)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** contexts approved (context map declined; canvases deferred to the next session at the expert's request)
- **Input:** `velocity/velocity-processes.jpg` (event-storming board screenshot)
- **Context map:** not requested ("No context map for now. Let's stop at the contexts today.")
- **Participants:** facilitator (Claude); domain expert (the trainer, so the reference solution is not a spoiler, but the boundaries below come from the expert's answers, not from `velocity/velocity-context-map.md`)

## Reading of the input
- **Main timeline**
  - *User Registration and Account Setup:* User Registered → Email Confirmed → Phone Verified → Basic Data Provided → Payment Method Added → Payment Method Verified → Agreement Signed → User Account Activated.
  - *Bike Rental:* User Location Validated (with a "bike broken" sticky) → Bike Reserved → Bike Unlocked / Bike Unlock Failed / Bike Reservation Expired. After Unlocked: Rental Session Started → bike returned, or Maintenance Started → bike fixed → bike returned.
  - *Pricing:* Rental Session Started → First Free Period Started/Ended → hourly billing periods with First/Second/Third Hour Fee Applied → Hourly Fee Applied → Rental Total Fee Calculated, or Max Rental Time Exceeded → Penalty Applied.
- **Branches (endings of the ride)**
  - *Station return:* Bike Returned To Station → Locked In Station → Fault Detected → Fault Reported, or Locking Failed → Customer Support Contacted / Bike Fault Reported.
  - *Zone return:* Bike Returned To Zone → Drop Zone Accepted → Drop Zone Fee Applied, or Unauthorized Zone Detected → Unauthorized Zone Return Fee Applied.
- **Disconnected clusters**
  - *Account Block and Debt Recovery:* Blocked Due To Invalid Card / Due To Debt → Block Notification Sent → Debt Recovery Started → Succeeded / Failed / Paused → Resumed → Succeeded / Failed.
  - *Customer Support and Incident Handling:* Incident Reported → Type Detected → Assigned To Agent → Accepted (→ Escalated → Resolved, or Resolved) / Rejected / Rejected → Reopened → Resolved.
- **Ambiguities raised:** duplicated Rental Session Started and "bike returned"; "bike broken" before Bike Reserved; Maintenance Started right after Unlock; no payment events; no trigger for debt or incidents.
- **Expert's corrections:** none to the reading. The duplicates are the same events (R1.2). "bike broken" = maintenance took the bike out of service; post-repair "bike returned" = back in the rentable fleet; Maintenance Started after Unlock = a technician takes the bike (R4.3, R4.4).

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| User Account Activated | confirmed | who the user is (registration, verification) | renting | "Activation happens once ... when the user has added a payment method. Validation happens before every rental" (R1.1) |
| Bike Reserved | rejected | - | - | "Reservation isn't a phase of its own ... reservation, validation and unlock have no extra rules of their own" (R1.2) |
| Rental Session Started | confirmed (phase change, not a boundary) | preparation (validation, reservation, unlock) | billable ride | "That's when the rider has actually rented the bike" (R1.2); H3 keeps both sides in Rental (no rules of their own) |
| Rental Session Ended (missing; return or forced end) | confirmed | ride | charging / settlement | "The rider is actually charged when they return the bike or are forced to end the session" (R1.3) |
| Rental Total Fee Calculated / "Hour Fee Applied" | rejected as pivot; renamed | - | - | "They're only estimates ... The estimate and the final charge use the same pricing rules" (R1.3) |
| Rental Unsettled (missing) | confirmed (trigger of debt cluster) | settlement | account block, debt recovery | "Due To Debt means a payment for a rental wasn't settled" (R1.4) |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Price estimate during the ride | lane (folds into Pricing) | ticks on its own hourly clock; same rules as the final price (R1.3, R2.2) |
| Station vs zone return | variant | "the same ride ending in two kinds of place ... except ... an extra fee" (R2.1) |
| Debt recovery | lane | own actions and schedule, pause/resume, long timeline (R2.3) |
| Incident handling | lane | several triggers, own loops (escalate, reject, reopen); nothing flows back (R2.5, R5.1) |
| Fleet maintenance | lane | "runs independently of rentals" (R1.5, R3.4); own triggers and rules (R4.1, R4.2) |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Price estimate vs final total | Pricing lane (hourly fees, total, penalty), zone fees on the return lanes | one Pricing context owns all price rules (R1.3, R2.2, R3.3) |
| Account blocking | onboarding (card not verified on time) and debt (unsettled payment) | Rider Accounts owns the blocked status and reasons; "The account answers 'why can't I rent?'"; each reason is lifted only by its own fix (R3.1). Settlement and Debt Recovery publish facts. Merging was not an option, because it would break the Account Activated pivot. |
| Charging vs recovering | "Payment" vs the debt cluster | Payment "doesn't know any rules" and is the external processor (R2.2, R3.2). Settlement owns charging with schedule rules (R2.2). Debt Recovery owns recovery with its own rules (R2.3). |
| Preparation vs ride | Bike Rental lane | preparation has no rules of its own (R1.2), so one Rental context |
| "Is this bike usable?" | "bike broken" in the rental lane, fault checks on return, incidents | Fleet Maintenance decides usability (R4.4). Rental checks the rider's report and publishes it (R1.5). Incident Handling validates the report and requests a repair (R4.1, R5.1). |
| Max rental time | board puts it in the Pricing lane | Rental ends the session (forced end). Pricing owns the penalty amount ("from the same price rules", R3.3). |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| Rider Accounts | Can this rider use the service? | User Registered … User Account Activated; payment-method verification deadline; blocked status with reasons (invalid card, debt) and reason-specific unblocks; Block Notification Sent; eligibility answer ("can rent / why not") | pivot: Account Activated; H3: single owner of blocking (R3.1) | not discussed |
| Rental | Who has which bike, and when? | User Location Validated (combines account status + bike availability, owns neither); Bike Reserved/Expired; Unlocked/Unlock Failed; Rental Session Started/Ended (incl. forced end at max time); station/zone returns (variants; zone authorization, lock confirmation); broken-bike check → Bike Fault Reported | pivots: Session Started/Ended; preparation folded in by H3 (R1.2) | not discussed |
| Pricing | What does this cost? | free period, hourly billing, hourly fees, drop-zone and unauthorized-zone fees, max-time penalty; Fee Estimated; Rental Total Fee Calculated | H2: estimate on its own clock; H3: "a simple calculator that anyone can use", changes independently (R2.2) | not discussed (likely supporting) |
| Settlement | What does the rider owe for this rental, and is it settled? | reaction to Rental Session Ended; charge order to the payment processor; schedule rules; Rental Settled / Rental Unsettled | H1: owner of the far side of the ride-end pivot; own rules "independent of the rental itself" (R2.2) | not discussed (likely supporting) |
| Debt Recovery | How do we recover what's owed? | Debt Recovery Process Started/Paused/Resumed/Succeeded/Failed; action and schedule rules; publishes recovery success (lifts the debt block in Rider Accounts) | H2: long timeline with pause/resume; H3: own rules (R2.3) | not discussed |
| Fleet Maintenance | Is this bike fit for service? | scheduled inspections, technicians' rounds, repair requests in; service intervals, checklists, repair priorities, retirement; Bike Taken Out Of Service, Maintenance Started, Bike Fixed, Bike Returned To Service, Bike Retired | H2: follows the bike, not the ride (R1.5, R3.4); own rules (R4.2); decides usability (R4.4) | not discussed |
| Incident Handling | What went wrong, and is it resolved? | Incident Reported, Type Detected, Assigned To Agent, Accepted/Rejected/Escalated/Reopened/Resolved; validates rider fault reports; Repair Requested to maintenance | H2: several triggers (rider contact, Rental's Bike Fault Reported), own loops; nothing else flows back (R2.5, R5.1) | not discussed |

**External party:** Payment Processor ("Payments"): "just the external payment processor seen from our side. We don't build it or change it" (R3.2).

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Rider Accounts | 1 | AGREE | none. Notes: placement rests on improvised answers (R2.4, R3.1); open: is activation before or after card verification? |
| Rental | 1 | AGREE | none. Notes: Rental owns the max-time limit and forced end, zone authorization and lock confirmation (not "rule-less"); move the maintenance stickies off the rental lane. |
| Pricing | 1 | AGREE | none. Notes: Pricing owns the penalty tariff only; the caller supplies the duration (stateless calculator); rename "Penalty Applied" to "Penalty Calculated". |
| Settlement | 1 | AGREE | none. Open: is Settlement's schedule about charge retries, and where exactly does it hand over to Debt Recovery? |
| Debt Recovery | 1 | AGREE | none. Open: what starts recovery (Rental Unsettled vs the block), what follows the final failure, does recovery settle the rental? |
| Fleet Maintenance | 1 | DISAGREE | 1) nothing triggers maintenance, and nobody owns "needs repair"; 2) availability depends on missing out-of/back-in-service events. Put 4 expert questions (round 4). |
| Fleet Maintenance | 2 | AGREE | resolved by R4.1-R4.4: own triggers and rules, single owner of usability, new events. Note: confirm the Incident → Maintenance repair request, since it reverses R2.5 (confirmed in R5.1). |
| Incident Handling | 1 | AGREE | none. Notes: Customer Support is a channel, not a context; Bike Fault Reported stays in Rental. |
| Incident Handling | 2 | AGREE | re-run because ownership changed (Repair Requested). The agent validates the report; Maintenance decides usability, so there's one owner each. |

## Context map
Not requested (R5.2). Notes from the critics for a future map session (not decisions):
- Fleet Maintenance is a likely upstream of Rental (out-of/back-in-service facts) and of Incident Handling (repair-request API). Rental is a likely upstream of Incident Handling (Bike Fault Reported). The critic saw no cycle there.
- Debt Recovery should be triggered by Rental Unsettled, not by Account Blocked Due To Debt, as the board draws it. Confirm this with the expert.

## Q&A with the domain expert
### Round 1: reading + H1 (pivotal events)
**Facilitator:**

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

**Expert:**

1. Activation happens once: the account is activated when the user has added a payment method. Validation happens before every rental, so that blocked or banned users can't rent.

2. *Rental Session Started* starts it. That's when the rider has actually rented the bike, the ride begins and price calculation can start. Reservation isn't a phase of its own. It just lets you reserve a bike before renting, and reservation, validation and unlock have no extra rules of their own. The duplicated stickies are the same events; they just appear in different flows.

3. They're only estimates, a convenience so the rider can see what the ride costs. The rider is actually charged when they return the bike or are forced to end the session. The estimate and the final charge use the same pricing rules.

4. There are two causes. *Due To Debt* means a payment for a rental wasn't settled, so that's where the debt comes from. *Due To Invalid Card* means the payment method wasn't verified on time.

5. Faults and broken bikes are probably related to incidents. When a bike is returned to a station or a zone, the rider can report it broken. The rental side checks that and publishes an event, and incident handling picks it up. The rental side doesn't care what happens after that. Maintenance runs independently of rentals; it isn't triggered by a ride.

### Round 2: confirmed pivots + H2 (swimlanes)
**Facilitator:**

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

**Expert:**

1. It's the same ride ending in two kinds of place. The rules are the same, except that returning the bike in an unauthorized zone costs an extra fee.

2. Settlement works out what's owed and makes sure it's paid. It has its own rules, mostly about the schedule, and they're not very complex. They're independent of the rental itself. Pricing is just a simple calculator that anyone can use. Payment doesn't know any rules; it just executes what it's told.

3. Yes. Debt recovery has its own rules about which actions to take, and its own schedule. When recovery succeeds, the account is unblocked so the rider can rent again. [improvised]

4. An account gets blocked for two reasons: an unsettled payment, or a payment method that wasn't verified on time. Each reason comes from the people who handle that problem. Either way the account ends up blocked, and the check before every rental is what stops the rider. [improvised: who decides]

5. No. A resolved incident doesn't flow back into rentals, payments or maintenance. Maintenance runs independently of rentals, but a bike that's being repaired can't be rented. [improvised: availability]

### Round 3: H3 (single responsibility)
**Facilitator:**

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

**Expert:**

1. The account answers "why can't I rent?". It shows that it's blocked and gives the reason. Each reason is lifted only by its own fix: recovering the debt lifts the debt block, and verifying the card lifts the card block. Recovering a debt never lifts a card block. [improvised]

2. Payments is just the external payment processor seen from our side. We don't build it or change it, and it doesn't know any of our rules. [improvised: external processor]

3. When the maximum rental time is exceeded, the session is forcibly ended. That's when the rider gets charged, the same as on a normal return. The penalty amount comes from the same price rules as every other fee. [improvised]

4. Only incident handling hears about it. Maintenance runs independently of rentals.

### Round 4: critic's questions on Fleet Maintenance
**Facilitator:**

The critics have reviewed all seven candidates. Six agree as they stand: Rider Accounts, Rental, Pricing, Settlement, Debt Recovery and Incident Handling. One disagrees: **Fleet Maintenance**.

The critic's point is that nothing on the board, and nothing you've said, starts maintenance. You said fault reports go only to incident handling and resolved incidents send nothing back. So "Is this bike in working order?" has no owner for its key decision ("this bike needs repair"). The "bike can't be rented while it's repaired" rule also depends on events the board doesn't have. I agree with that, and it needs your facts, not a guess:

1. What starts maintenance on a bike: a schedule or inspection, a technician's round, a fault the bike or dock detects, or something else? (Decides whether Fleet Maintenance owns the "needs repair" decision, and whether it needs a link to Rental or Incident Handling.)
2. Does maintenance have its own rules, such as service intervals, checklists, priorities or retiring bikes? Or does it only record "broken" and later "fixed"? (Decides whether it's a context or just a status flag on the bike.)
3. On the board, "maintenance started" comes right after "Bike Unlocked". Does a technician unlock the bike to take it away, or can a rider's ride end in maintenance? (If a ride can end in maintenance, "independent of rides" no longer holds.)
4. What does the "bike broken" sticky before Bike Reserved mean, and what does "bike returned" after "bike fixed" mean? Is it the bike going back into the rentable fleet? (Names the events the availability rule depends on, and who decides a bike is unusable.)

**Expert:**

1. Maintenance runs on its own: scheduled inspections and technicians' rounds. Separately, a rider can report a bike broken when returning it to a station or zone. That report goes to incident handling, and if the agent finds the bike really is faulty, they ask maintenance to repair it. [improvised: inspections, rounds, agent's repair request]

2. Yes, maintenance has its own rules: service intervals, inspection checklists, repair priorities, and when to retire a bike. It's more than "broken" and then "fixed". [improvised]

3. A technician unlocks the bike to take it away. A rider's ride never ends in maintenance; the rider always returns the bike first. [improvised]

4. "Bike broken" means maintenance has taken the bike out of service, so nobody can reserve or rent it. "Bike returned" after "bike fixed" means maintenance has put the bike back into the rentable fleet. Maintenance decides whether a bike is usable. [improvised]

### Round 5: candidate approval and context map
**Facilitator:**

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

**Expert:**

1. Yes, I approve the seven contexts. And yes, I confirm it: when an incident agent confirms that a reported bike really is faulty, they ask maintenance to repair it. That replaces my earlier "nothing flows into maintenance". Resolved incidents still send nothing back.

2. No context map for now. Let's stop at the contexts today. We'll do canvases in the next session.

## Open questions and assumptions
- **Improvised answers.** The expert flagged these as improvised, and the boundaries rest on them:
  - who blocks and unblocks (R2.4, R3.1)
  - debt recovery success unblocking (R2.3)
  - Payments being an external party (R3.2)
  - the forced end and penalty (R3.3)
  - all of maintenance's triggers, rules and availability (R2.5, R4.1-R4.4)
- **Changed answer.** R2.5 said nothing flows into maintenance. R4.1 says incident agents request repairs, and R5.1 confirmed the later version.
- **Open, non-blocking (from the critics):**
  - Is the account activated before or after the card is verified?
  - Is Settlement's schedule a charge-retry schedule, and when exactly does it hand over to Debt Recovery?
  - What starts recovery, and what follows its final failure (write-off, external agency, permanent ban)?
  - Does a recovered debt mark the rental settled?
  - Does a station Locking Failed still end the session?
  - Does an incident stay open until the repair is done?
- **Classification** (core/supporting/generic) wasn't discussed.

## Suggested board changes
- Add: Rental Session Ended (with a forced-end variant), Payment Succeeded / Payment Failed, Rental Settled / Rental Unsettled, Debt Recovered, Repair Requested, Inspection Done, Bike Retired.
- Rename:
  - "Nth Hour Fee Applied" / "Hourly Fee Applied" → "Fee Estimated"
  - "Penalty Applied" → "Penalty Calculated"
  - "bike broken" → "Bike Taken Out Of Service"
  - post-repair "bike returned" → "Bike Returned To Service"
- Move Maintenance Started / Bike Fixed / Bike Returned To Service from the Bike Rental lane into their own Fleet Maintenance lane.
- Treat the two "Incident Rejected" stickies as one event with an optional reopen.

## Next steps
- Next session (as the expert asked): canvases via `bounded-context-scaffolder`, one per context at `docs/bounded-contexts/velocity/<context-package>/canvas.md`. Pre-fill them from this report. Then optionally a context map (`src/main/cml/VeloCity-Context-Map-<slug>.cml`; don't overwrite the reference `VeloCity-Context-Map.cml`).
- Resume point: this report. Status `contexts approved`.
