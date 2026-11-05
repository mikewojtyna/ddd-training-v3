# Transcript

## User (initial task)

"can you sanity-check the VeloCity context map in src/main/cml/VeloCity-Context-Map.cml? I wrote it as the reference solution for the workshop and I want to know if it breaks any of our context mapping rules before I show it to the group."

## Facilitator -> user, message 1

I'll review the map as a validation, not a second discovery, and I won't touch your `.cml`. Any corrections will go into a separate file.

**How I read it.** There are 8 internal contexts (Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport, FleetMaintenance), 2 external parties (PaymentProcessor, City) and 11 relationships. One of them, CustomerSupport → FleetMaintenance, is marked unconfirmed. The CML parses cleanly: 0 errors. The validator raises 11 rule prompts: 8 Conformists to justify, 2 downstreams with neither ACL nor CF (Rentals from Accounts, and FleetMaintenance from CustomerSupport), and one indirect cycle, Accounts → Rentals → Settlement → Accounts.

**The processes I'll test the map against**, taken from the vision:
- A. Onboarding, up to activation (payment method verified through the processor)
- B. Rent and return a bike (eligibility, reserve, unlock, session, station or zone return, max-time forced end)
- C. From a finished ride to money (price, charge, retries, outstanding debt, block)
- D. Debt recovery (reminders, payment plans, pause/resume, success leads to unblock)
- E. Broken bike to back in circulation (fault detected, technician reserve/unlock, maintenance, fixed)
- F. Incident handling (report, route, accept/reject, escalate, resolve), including fleet issues and billing disputes
- G. Invalid card leads to a block

## Questions

1. **Processes.** Is A–G the right test bench? Is any process missing, or wrongly cut? This decides which hand-offs need a relationship.
2. **Fleet-issue incidents.** When support handles an incident about a faulty bike, what happens to the bike next, and who decides that it's broken? This decides the unconfirmed CustomerSupport → FleetMaintenance link.
3. **Billing disputes.** When a rider disputes a charge through support, what happens to an ongoing debt recovery, and who decides that? Nothing on the map carries this today.
4. **Debt blocks.** When a ride's fee can't be collected, who decides that the account is blocked, and who decides that it's unblocked? Your doc says "Settlement drives both block and unblock", and the discovery session says Accounts owns all blocking rules. This decides the direction of one link in the indirect cycle.
5. **Teams and languages.** Are all eight internal contexts built by teams in the same organisation? Do you want the downstream contexts to adopt Payments', Rentals', Pricing's and Settlement's models as they are (Conformist)? Or should they translate them? Our rules allow CF only towards a well-established language, so this decides 8 of the 11 rule prompts.

## Expert reply 1

**1. Processes.** The bench is close, but I'd cut it differently. I think of VeloCity as four end-to-end processes:
- **Onboarding and activation** (your A).
- **Rent and return a bike** (your B).
- **Settle a ride and recover the debt** (your C and D together). For me this is one process. It starts when a ride ends and finishes when the money is in, the debt is written off, or recovery is exhausted. The quick retries right after the ride and the later active pursuit are two stages of the same thing.
- **Bike fault to maintenance** (your E).

Incidents (F) aren't a separate value stream for us. Support takes in what riders tell us and feeds it into those processes: a faulty bike goes into the fault-to-maintenance flow, and a disputed charge goes into the settle-and-recover flow. Still, check the hand-offs from support. An invalid card (G) isn't a process either. It's something that can happen to an account at any time, and its only effect is a block. It's fine as a test scenario.

**2. Fleet-issue incidents.** The rider reports the faulty bike, and the incident is detected as a fleet issue and assigned to an agent. If the agent accepts it, that counts as confirming the fault. From then on the bike is broken, it isn't offered to riders, and it goes to the technicians' queue like any other broken bike: reserve, unlock, maintenance, fixed, back in circulation. If the agent rejects the report, the bike stays in service. So the support agent decides that the bike is broken. The technicians decide when it's fixed and back in circulation, and support has no say in that part. For support, the incident is resolved once the bike has been handed over to the technicians. Support doesn't wait for the repair.

**3. Billing disputes.** While a dispute about a charge is under review, debt recovery for that debt is paused: no reminders and no collection attempts. The rider stays blocked during the pause, because the money is still owed. The agent, or the specialised billing team if the case is escalated, decides to pause and decides the outcome. If the dispute is rejected, recovery resumes from where it stopped. If it's upheld, the disputed amount is cancelled. The rider then no longer owes it, and if that was all they owed, the block is lifted. Recovery is never stopped silently. It ends only with paid, exhausted, or cancelled after an upheld dispute.

**4. Debt blocks.** Both documents are right, about different things:
- **Who states the facts.** The people who charge rides and keep the rider's balance say "this ride's fee could not be collected", once the automatic retries are used up. Later they say "the outstanding debt is cleared". The debt counts as cleared only when it's settled on the rider's balance. That's true even when the money comes in through a payment plan or after a reminder. The recovery team chases the rider, but the money is booked against the balance, and only then is the debt cleared.
- **Who decides block and unblock.** The account rules belong to the accounts team. They block the account when they hear the fee is unpaid, and they unblock it when they hear the debt is cleared. The same goes for an invalid card: the payment side reports it, and the accounts team blocks. Nobody else blocks or unblocks an account directly.

**5. Teams and languages.**
- **Organisation.** All eight internal teams are in the same company. Only the payment processor and the City are outside.
- **Your choice.** I'd have the downstream teams adopt the models as they are, not translate them. Those words are used the same way across the whole company:
  - Payment method, verified, invalidated and charge.
  - Rental session ended, with start, end, location and reason.
  - The price of a session.
  - Ride fee unpaid and outstanding debt cleared.

  None of them has changed meaning since we started, and nobody downstream uses them differently.

## Facilitator -> user, message 2

Thanks. Here's what your answers change.

- **Test bench:** I'll use your four processes: onboarding, rent and return, settle and recover, and fault to maintenance. Support's hand-offs get checked inside the processes they feed, and the invalid card is a test scenario.
- **Fleet issues are confirmed.** Support decides a bike is broken, and Fleet Maintenance owns everything after that. So the CustomerSupport → FleetMaintenance link stays, Support is upstream, and it's no longer an assumption.
- **The debt block is consistent with the CML.** Settlement states the facts, and Accounts decides block and unblock. The Settlement → Accounts arrow is correct. The doc's sentence "Settlement drives both block and unblock" is misleading and should be reworded.
- **The indirect cycle** (Accounts → Rentals → Settlement → Accounts): every link now rests on a fact you've stated, so I'll check the flips and report back. I won't relabel anything without a reason.
- **Billing disputes are a real gap.** Support pauses, resumes and ends recovery, and nothing on the map carries that today.

## Questions

1. **Upheld dispute.** When a dispute is upheld and the amount is cancelled, who books that cancellation against the rider's balance? How does debt recovery learn that it ended as "cancelled"? This decides which contexts Support is upstream of.
2. **The specialised billing team.** Escalated billing disputes go to this team. Which of the eight teams is it: Support itself, the team that charges rides and keeps the balance, or another one? If it isn't Support, the dispute decision has a second owner.
3. **Bike availability.** When Fleet Maintenance withdraws a bike or makes it available again, and when technicians reserve and unlock bikes, do they use Rentals' words ("available", "withdrawn", "reserve", "unlock") exactly as they are, the same way across the company? That isn't on your list of shared terms, and the map has Fleet Maintenance conforming to them.
4. **Debt collection and account status.** The same question for two more links. Does Debt Recovery adopt Settlement's "attempt collection" and "outstanding debt" as they are? And when Rentals checks whether an account is active, does it use Accounts' model as it is, or translate it? The map gives this second link no pattern at all.

## Expert reply 2

**1. Upheld dispute.** The people who charge rides and keep the rider's balance book the cancellation. Support decides that the dispute is upheld and for which amount, then tells the balance people to cancel it. Nobody else touches the balance. Once the cancellation is booked, the balance people announce that the debt is cleared and say how: paid or cancelled. Debt recovery learns from that same announcement that its case is over, the same way it learns that a debt was paid. Support doesn't close recovery cases itself. Support does tell recovery to pause when a dispute opens and to resume when a dispute is rejected.

**2. The specialised billing team.** It's part of customer support: support agents who specialise in billing disputes. It isn't the team that charges rides. They look at the rider's charges and balance, but the decision on the dispute is theirs, as support. So the dispute decision has one owner, support, whether or not the case was escalated.

**3. Bike availability.** Yes. Technicians reserve and unlock a bike in exactly the same way and with exactly the same words as riders. Only what happens afterwards differs: a ride for a rider, maintenance for a technician. Among themselves the maintenance people talk about "broken", "fixed" and "back in circulation". When they take a bike out of the offer or put it back, they use the rental side's words, "withdrawn" and "available", as they are. Those words have meant the same thing across the company since launch, and nobody uses them differently. I left them off my earlier list only because you didn't ask about them.

**4. Debt collection and account status.**
- **Debt collection.** Yes, the recovery team uses "outstanding debt" and "attempt collection" exactly as the balance people define them. Collecting always means asking the balance people to charge. Their own words are for the pursuit: reminders, payment plans, paused, resumed, exhausted.
- **Account status.** The rental side only wants to know one thing: can this person rent right now? They don't care why an account can't rent, whether onboarding is incomplete or the account is blocked. "Active" means activated and not blocked, exactly as the accounts team defines it, and the rental side takes that answer as it is. They don't translate it.

## Facilitator -> user, message 3

That settles the disputes. Support owns the dispute decision, the balance people (Settlement) book the cancellation, and recovery learns the outcome from "debt cleared (paid or cancelled)". So the proposed map gets two confirmed links that the reference map lacks:
- Settlement → Support, for cancelling a disputed amount. Support asks, in Settlement's language, through Settlement's service.
- DebtRecovery → Support, for pause and resume. Support asks, in Debt Recovery's language.

Your Conformist answers justify CF on every internal link, and Rentals → Fleet Maintenance and Accounts → Rentals are now CF with a reason.

One more gap from walking "rent and return":

1. **The live estimate.** During a ride, the rider sees a running estimate of the price. Which team produces that estimate for the rider, and where does it get the price from? The map has only Pricing → Settlement, so nothing carries the estimate today. This decides whether a Pricing → Rentals link is missing.

## Expert reply 3

Domain expert answer on the live estimate: there isn't one. Riders don't see a running price estimate during a ride.

The rider learns the price when the session ends: the total fee is calculated at that point, from the tariff that was valid when the ride started, and then the ride is charged. During the ride the app shows that a session is running and how long it has lasted, and nothing more. The free period and the hourly tiers are published, so a rider can work the price out, but we don't calculate a price for them mid-ride. Nobody has asked for that at launch, so there's nothing to carry today.

## Critic verdicts (context mode, round 1)

- Rentals, round 1: DISAGREE. No pivot or lane issues, but who owns the decision to withdraw a bike on a fault the rider reports at return is open: support-reported faults need an agent's acceptance, while Rentals withdraws on the rider's word at return. Expert question raised.
- FleetMaintenance, round 1: DISAGREE. The CML says FleetMaintenance owns "broken", but the expert gave the "broken" decision to Support. Restate it as reacting to confirmed-fault facts and owning the return to circulation. The same question about faults reported at return, plus who receives the bike's own fault signals.

## Facilitator -> user, message 4

No live estimate, so no link is missing there. Pricing → Settlement is enough.

The independent critics reviewed Rentals and Fleet Maintenance, the two contexts I flagged because the "broken / withdrawn" decision is spread across them. Both agree the boundaries hold: no straddled pivot, and maintenance is a real lane. Both stopped on the same gap. You told me a fault reported **through support** makes the bike broken only once an agent accepts it. But the reference map has Rentals withdraw a bike on a fault reported **at return**, on the rider's word alone. If those two need different rules, then the withdrawal decision has two owners.

## Questions

1. **A fault reported at return.** When a rider reports a fault while returning a bike, at a station or in a zone, does the bike stop being offered right away? Or does someone first have to confirm it, the way an agent accepts a support report? If it can be rejected, who puts the bike back into service? This decides whether Rentals owns this withdrawal or only reports the fault.
2. **The bike's own signals.** When the bike itself signals a mechanical fault, as opposed to a failed lock, who receives that signal, and is the bike out of service right away? The reference map lists this as an unconfirmed default in Rentals.
3. **After an agent accepts a fleet issue.** Must the bike come off offer at the moment the agent accepts it? Or is it acceptable that it leaves the offer only once the technicians have it in their queue? This decides whether Support needs its own link to Rentals, or whether going through Fleet Maintenance is enough.

## Expert reply 4

**1. A fault reported at return.** The bike stops being offered right away, and nobody confirms anything first. The rule is the same at a station and in a drop zone. The difference from support is about what we know. At return, the rider is standing at that exact bike and has just ridden it, so we know which bike it is and that the rider has had it in hand. We take the rider's word, because offering a possibly broken bike to the next rider is worse than losing a working one for a few hours. Nothing rejects the report. If the technician later finds nothing wrong, they mark the bike fixed and put it back into circulation, the same as after any repair. The decision to take the bike out and the decision to put it back stay with different people.

**2. The bike's own signals.** The station or the bike's lock reports the fault, so the same people who run stations, locks and returns receive it. A mechanical fault is treated the same way as a failed lock: the bike is out of service immediately, with no confirmation. A machine signal is not something anyone accepts or rejects. From there it goes to the technicians like any other broken bike.

**3. After an agent accepts a fleet issue.** The bike must come off offer at the moment the agent accepts. A broken bike is never offered to riders, and we won't accept a gap where it could be rented after we've confirmed the fault. Accepting the incident and handing the bike to the technicians are the same moment for us. If the bike is already in the technicians' queue at that moment, then it's already off offer and I'm happy. What matters is that there's no window in between.

Why a support report needs acceptance at all: it often arrives later, from a rider who no longer has the bike, and it can be vague or point at the wrong bike. The agent's acceptance is what ties the report to a specific bike and confirms there is a real fault.

- Rentals, round 2: AGREE. Fix (a) confirmed by the expert. Rentals withdraws on faults in its own flow, and only FleetMaintenance puts a bike back. Non-blocking: is a few seconds of delay between an accepted fleet issue and the bike leaving the offer acceptable?
- FleetMaintenance, round 2: AGREE. One question now ("is this broken bike fixed, back in circulation?"), and "broken" is decided by Support or Rentals for their own sources. Non-blocking for the map: does the handover need to be synchronous (no window), and whose words does it use?
- Map (all processes), round 1: DISAGREE. The direction of CustomerSupport -> FleetMaintenance may be wrong (Support needs confirmation of the handover, and there must be no window), so FM might offer an OHS instead. City CF is unconfirmed. Questions on partial disputes, refunds and reminder contact data. Original map findings: 2 relationships without a downstream pattern, 8 CFs not justified, an undocumented indirect cycle (the write-up says "No cycles"), no relationships for dispute hand-offs, and misleading wording on blocking.

## Facilitator -> user, message 5

The critic on the whole map, walking your four processes, agreed with almost everything. It raised one question that could flip an arrow, and a few smaller completeness questions.

## Questions

1. **The fleet handover.** When an agent accepts a fleet issue, how does the agent know the bike has reached the technicians? Does the technicians' side confirm it back? And when the bike is handed over, is it described in the technicians' terms (a broken bike, a fault) or in support's terms (an accepted incident)? This decides which way the Support–Fleet Maintenance arrow points.
2. **The City's zone map.** Is the City's zone map a stable, well-defined format that pricing can use as it is, or does it arrive in a form we have to interpret? Does anyone besides pricing need it, for example to check the service area when a rental starts? This decides whether the Conformist towards the City is justified and whether a link is missing.
3. **Partial and paid disputes.** Can a dispute be upheld for only part of a debt? If so, what happens to the paused recovery for the rest? And can a rider dispute a charge that was already paid? If so, who gives the money back? This decides what flows on the existing links. It shouldn't add new ones.
4. **Reminders.** Debt recovery sends reminders to the rider. Whose contact details does it use, and who keeps them? This decides whether a link from Accounts to Debt Recovery is missing.

## Expert reply 5

**1. The fleet handover.** The agent hands over a broken bike, in the technicians' terms: which bike it is and what the fault is, for example "bike 4711, front brake doesn't work". The technicians don't get the incident and don't need it. Who reported it, the conversation and the accept/reject history all stay with support. Yes, the technicians' side confirms back: the bike is now broken and in their queue, which also means it's off offer. That confirmation is the moment the agent marks the incident resolved. After that, support hears nothing more about the bike.

**2. The City's zone map.** The City publishes it in its own format, and we can't change it. The format is fixed and documented. It lists the stations and the approved drop zones, and everything else is an unauthorized zone. It changes a few times a year, when the City approves or removes zones, and we take each version as it comes. At launch, only pricing needs it, to decide the drop zone fee versus the unauthorized zone fee. The service area check at the start of a rental is a different thing. The service area is VeloCity's own decision about where we operate, not the City's map, and the rental side checks against it. Long-term fleet distribution will use the zone map one day, but nobody does that at launch.

**3. Partial and paid disputes.**
- **Partial:** yes, a dispute can be upheld for part of a debt. A typical case is removing the penalty while the hourly fees stand. The balance people book the cancelled part. For the rest, recovery resumes from where it was paused, now pursuing the smaller amount.
- **Paid:** yes, a rider can dispute a charge that was already paid. If it's upheld, support decides the amount and the balance people give the money back to the rider's payment method. Only the balance people move money, in both directions. There's no recovery involved, because nothing is owed.

**4. Reminders.** The rider's email and phone number are collected and verified during onboarding. The accounts team keeps them, and the rider changes them in their account. Debt recovery always uses the current, verified contact details at the moment it sends a reminder. It doesn't keep its own list, because a reminder sent to an old number is useless.

- Map (all processes), round 2: AGREE. The flipped FM -> Support handover, the City CF justification, the dispute messages and Accounts -> DebtRecovery contacts all hold. Only the documented indirect cycle remains, and it is justified. Notes: mention refunds and partial cancellation in Settlement.

## Facilitator -> user, final message

Your reference map parses cleanly (0 errors), and all eight contexts hold up under the three heuristics. As written, though, it breaks several of our context-mapping rules:

1. **8 Conformists with no justification** (CF only towards a well-established language). Your answers justify all of them: one company, shared and stable terms. That justification just isn't written anywhere yet.
2. **Two downstreams with neither an ACL nor a CF:** Accounts → Rentals, and Support → Fleet Maintenance.
3. **An indirect cycle, Accounts → Rentals → Settlement → Accounts,** while the write-up says "No cycles". I tried flipping each link, and every flip contradicts something you stated. So it's a legitimate loop (an unpaid ride blocks future rentals), but it has to be documented.
4. **Support → Fleet Maintenance points the wrong way.** The handover uses the technicians' words and they confirm it back, so Fleet Maintenance is upstream, through a handover service.
5. **The billing-dispute hand-offs have no relationships:** pause/resume to Debt Recovery, and cancel/refund to Settlement. Neither does the rider's contact details for Debt Recovery's reminders.
6. **Misleading wording.** "Settlement drives block and unblock" should say that Accounts alone decides. Two accepted alternatives contradict your own answers.

I didn't touch your file. Paths:
- Report: `docs/context-discovery/velocity/2026-11-05-reference-map-validation/discovery.md`
- Proposed map: `src/main/cml/VeloCity-Context-Map-validated.cml`. It's valid, with 10 contexts and 14 relationships. The only rule line left is the documented cycle.

What the critics changed:
- **Rentals and Fleet Maintenance:** clarified who withdraws a bike (Rentals, for faults in its own flow) and who puts it back (Fleet Maintenance only).
- **Map, round 1:** flipped the Support/Fleet link, and turned up partial disputes, refunds and reminder contacts.
- **Map, round 2:** approved.

Still open:
- the ACL defaults on the four new links
- who resumes recovery after a partly upheld dispute
- where Rentals learns who is a technician

Do you want the proposal to replace the reference CML, with `velocity-context-map.md` updated to match? Once the map is approved, I can also hand the contexts to bounded-context-scaffolder for canvases.
