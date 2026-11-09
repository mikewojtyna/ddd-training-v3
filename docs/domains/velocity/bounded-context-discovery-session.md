Ok, let me give you just 3 heurstics I often use.

1. Pivotal events => look for events that are changing the business phases. For example, Order Finalized. It's prepared by order preparation context, and consumed by order delivery context. Events before this pivotal event often are from different context, while after pivotal are another context. You can identify pivotal events by asking questions such as: "what it means" or "what happens before that",  "what happens after that". If you confirm the event classified as pivotal (e.g. "separating important business phases"), it a strong indication what happens before it is another context from what happens after it. Use pivotal events always as a first heuristic. It's not always enough, though. That's why you need to also apply 2 other heuristics.
2. Swimlanes. If you identify certain flow of events are happening in parallel, are often repeating or you cannot easily get an answer whether some events happens before or after another one, ordering is not linear, this is another strong indication that these parallel flows are from different context. Again, remember to always ask me questions to clarify the meaning of events and to extract the right boundaries and domain complexity. Use swimlanes as your second heuristic, after pivotal events.
3. Single responsibility. If the same responsibility (e.g. making an order) is distributed among different bounded contexts, it's likely that these contexts are in fact a single context and therefore should be merged. Note, that this heuristic assumes you already have good candidates from swimlanes and pivotal events. So, use it as a validation tool. Different contexts cannot answer the same question/have the same responsibility. Use it as a third heuristic to validate your initial assumptions. Remember, to ask me questions.


I've mentioned you need to ask me questions. Remember to do add, to clarify the meaning, ordering and boundaries. Miro screenshots are not enough.

Ok, don't do anything yet. Wait for my screenshots and then you will start searching four bounded contexts according to the 3 heuristics I've described to you.

===

I've read through the whole board. Before I go further, here is my reading of it, so you can correct anything I've misread.

The main timeline runs from onboarding (User Registered through User Account Activated) to pre-rental checks (Account Validated, User Location Validated, bike broken). Then come the reservation and unlock steps, and the ride splits into three branches: hourly billing, return to station, and return to zone. Separately, there are two disconnected clusters at the bottom. One is debt recovery, which starts from Account Blocked Due To Debt. The other is incident handling, from Incident Reported through Resolved/Rejected/Reopened.

## Heuristic 1: candidate pivotal events

These are the events that look like they separate business phases. None is confirmed yet. Each needs your answer about what it means and what happens before and after it.

**User Account Activated.** Before it, everything is about establishing who the user is and whether they can pay. After it, everything is about bikes and rides. This looks like the strongest candidate on the board.

**Bike Reserved or Rental Session Started.** I can't tell yet which of these is the real phase change. They might also be two separate pivots, with reservation as a short phase of its own.

**Bike Returned To Station / Bike Returned to Zone.** This seems to end the ride and start settlement. But billing hangs off Rental Session Started, not off the return, so I'm not sure what the return actually triggers.

**Rental Total Fee Calculated.** This looks like the hand-off from the ride to payment. Nothing on the board follows it, though.

**Account Blocked Due To Debt** and **Incident Reported.** Both start their own flows, but nothing on the board connects them to the main timeline, so I can't yet tell what triggers them.

## Questions

1. **Account Activated vs. Account Validated.** What's the difference between them? Is validation a check repeated before every rental, while activation happens once? If so, onboarding ends at activation and validation belongs to the rental side.

2. **Reservation and the start of the ride.** What does Rental Session Started mean in business terms? Is it the moment the ride is billable? Also, does Bike Reserved mark a meaningful phase of its own, for example with its own rules, time limits and fees? Or is it just the first step of renting?

3. **What ends a ride, and what comes after it.** What does a return trigger? Billing seems to run during the ride, so does the return stop the clock and produce Rental Total Fee Calculated? Do the drop-zone and unauthorized-zone fees feed into that total? And what happens after the total is calculated? I'd expect a charge that can fail, which is presumably where Account Blocked Due To Debt comes from, but that link is missing on the board.

4. **Broken bikes and faults.** Three things on the board confuse me:
   - "bike broken" sits in parallel with the account checks, before Bike Reserved.
   - "maintenance started → bike fixed → bike returned" hangs off Bike Unlocked.
   - Bike Fault Reported and Customer Support Contacted appear after a return.
   
   Do any of these lead to Incident Reported? Is maintenance something the rider's flow triggers, or is it a separate operational process that just happens to touch the same bike?

5. **Possible duplicates.** Rental Session Started appears twice, and Account Blocked appears both on its own and as "Account Blocked Due To Invalid Card". Is each pair the same event?

Once you answer these, I'll confirm the pivotal events and move on to swimlanes. The billing hours, the two return paths, and the debt and incident flows already look like strong parallel-flow candidates.

===

1. Account is activated when user added some payment method. Account needs to be validated before every rental, so we make sure blocked/banned users are not eligible to make a rental.
2. Rental Session Started means that renter has rented a bike , the ride has started and price calculation can start. Bike reserveation is not a good phase candidate. IT just allows us to make a reservation before the rental.
3. Actually, the physical payment/billing is made when rider returns the bike or is forced to end the session. What happens during the ride is nothing more than a convenient estimation for the rider, so she's able to estimate the total price.
4. I guess so, these can be related to incidents.
5. These events are duplicated, because they can happen in different flows. But they mean the same thing.

===

Thanks, that clears things up. Here's where heuristic 1 lands.

## Heuristic 1: confirmed pivotal events

**User Account Activated** separates onboarding from renting. Activation happens once, when a payment method is in place. Account Validated is a different thing: an eligibility check that runs before every rental. So I'm placing it on the rental side of the boundary, even though it reads account data. I'll come back to who owns the blocked/banned status under heuristic 3.

**Rental Session Started** separates preparing a rental (validation, reservation, unlock) from the ride itself. Reservation is just a convenience step before this pivot, not a phase of its own.

**Bike returned, or session forcibly ended,** separates the ride from settlement. This is where the real fee is calculated and the rider is actually charged.

That gives four phases: onboarding, rental preparation, ride, and settlement. Two things are missing from the board that matter for this boundary. There's no event for a forced end of the session, and nothing after Rental Total Fee Calculated (something like Payment Charged or Payment Failed). I'd add both, because the second one is probably what leads to Account Blocked Due To Debt.

Your answer also changes how I read the hourly fee events. "First Hour Fee Applied" sounds like charging, but those events are really price estimates shown to the rider. That suggests two separate things are happening around pricing: estimating during the ride, and calculating the real charge at the end. I'll keep that in mind for heuristic 3.

## Heuristic 2: candidate swimlanes

These are the flows that look like they run in parallel, repeat, or have unclear ordering:

- **Ride vs. price estimation.** The hourly estimation ticks along on its own clock while the physical ride and return happen alongside it.
- **Bike maintenance.** "bike broken", "maintenance started → bike fixed → bike returned", and the fault reports all seem to follow the bike's condition, not any particular rental.
- **Incident handling.** It can be triggered from several places (locking failures, faults, customer support) and has its own loops (escalate, reject, reopen).
- **Debt recovery.** It can pause and resume, so it probably runs over days, on a completely different timeline from a ride.
- **Station vs. zone returns.** I can't tell yet whether these are two parallel lanes or just two endings of the same ride.

## Questions

1. **Station vs. zone.** Are docked stations and free-floating drop zones two different rental models, with different rules, fees and operations? Or is it one ride that can end in two kinds of places?

2. **Maintenance.** Does maintenance run independently of rentals, for example scheduled or triggered by ops staff on their own timeline? And does "bike broken" before Bike Reserved mean a bike in maintenance can't be reserved?

3. **Debt recovery.** Does it start only from a failed charge at settlement? While it runs, can the rider keep doing anything, or are they just blocked until it ends? And when it succeeds, what unblocks the account?

4. **Incidents.** Who reports incidents: riders through support, automatic fault detection, or both? When an incident is resolved, does anything flow back to another lane, such as a refund, a maintenance order, or an unblocked account?

5. **Pricing.** Do the live estimate and the final charge use the same pricing rules (hourly tiers, free period, penalties, zone fees)? Or does the final charge follow its own rules?

===

1. the same rules, except of extra fees being applied if bike is rental in an unauthorized zone.
2. Independently, but bike can be reported broken also when returning to a station or a zone.
3. account can be blocked only because of unsettled payment. Rider is not allowed to rent a bike when debt recovery kicked in for her.
4. Both. No connections.
5. Yes, the same rules. The only difference is that after bike being returned the payment should be initiated.

===

Here's where heuristics 2 and 3 land, and a first draft of the contexts.

## Heuristic 2: swimlanes, after your answers

**Station vs. zone returns: not a separate lane.** The rules are the same except for the unauthorized-zone fee, so these are just two ways a ride ends. They stay inside the ride.

**Maintenance: confirmed lane.** It runs independently of rentals, on its own timeline. The return flow only feeds it a fault report.

**Incidents: confirmed lane.** They come from riders and from automatic detection, and resolving one doesn't feed back into any other flow.

**Debt recovery: confirmed lane.** It runs on its own timeline. Its one effect on the rest of the board is that it makes the rider ineligible to rent.

**Price estimation: a separate lane from the ride.** It ticks along in parallel during the ride, but it uses the same rules as the final charge. That matters for heuristic 3.

## Heuristic 3: single-responsibility check

**Pricing is one responsibility, not two.** The live estimate and the final fee both answer the same question: "how much does this ride cost?" They use the same rules, and the zone fees belong to the same rules too. So the estimation lane and the final calculation must sit in one context. They shouldn't be split between "ride" and "settlement". Pricing still answers a different question from the ride itself, which answers "who has which bike, from when to when?" That's why I keep pricing as a separate candidate.

**Payment and debt are one responsibility.** Starting the payment after a return, a failed payment, Account Blocked Due To Debt, the block notifications and debt recovery all answer "does this rider owe us money?" They merge into one context. And since an account can be blocked only because of an unsettled payment, the "blocked" status belongs to this context too.

**The rental eligibility check doesn't own data, it combines answers.** Account Validated asks onboarding whether the account is active, and asks payments whether the rider has unsettled debt. That's consistent with keeping it on the rental side.

## Draft bounded contexts

1. **Rider Onboarding:** from User Registered to User Account Activated.
2. **Rental:** eligibility check, reservation, unlock, Rental Session Started, return to a station or zone, and forced end.
3. **Pricing:** free period, hourly tiers, max-time penalty, zone fees. It serves both the live estimate and the final total.
4. **Payments & Debt Recovery:** starting the payment, payment failure, account blocking, notifications, and the recovery process.
5. **Fleet Maintenance:** bike broken, fault detected and reported, maintenance started, bike fixed.
6. **Incident Handling:** from reported to resolved, rejected or reopened.

## Open questions

1. **Account blocking in onboarding.** The onboarding flow has "Payment Method Not Verified → Account Blocked", and there's also "Account Blocked Due To Invalid Card". If blocking happens only for unsettled payments, what are these? If they're the same kind of block as the debt one, heuristic 3 says onboarding shouldn't own blocking. If they mean something different, like "registration can't be completed", they'd stay in onboarding but deserve a different name.

2. **Rental preparation vs. the ride.** Rental Session Started is a confirmed pivot, which suggests what comes before it is a different context. But reservation, unlock and the ride all seem to answer "who holds this bike right now?", which points toward merging them. Is there anything in the preparation phase with its own rules, owners, or reasons to change, separate from the ride?

3. **Pricing as its own context.** Do pricing rules change on a different schedule, or get decided by different people (for example, tariffs set by business), compared with how rentals work? If they don't, I'd consider folding Pricing into Rental.

4. **Fault reported on return.** When a rider reports a broken bike while returning it, does that go straight to Fleet Maintenance, or does it go through Incident Handling first?

===

1. Well, I forgot that account can be also blocked because of payment not being verified on time. Unsettled payment is NOT the only way to block the account.
2. No extra rules in the preparation phase.
3. Pricing evolves independently. It's just a simple calculator, nothing more. Can be used by any other context/actor.
4. The rental context simply checks it and published an event. The event is being consumed by the incident handling, but rental context doesnt know about it.

===

Here's the updated picture.

**Rental preparation merges into Rental.** There are no separate rules before Rental Session Started, so validation, reservation, unlock and the ride form one context. The pivot still marks a meaningful phase change, but heuristic 3 says it isn't a context boundary here.

**Pricing stays separate.** It changes independently and is a plain calculator that Rental (for the live estimate) and Payments (for the final charge) both call. That makes it a clear, small supporting context.

**Fault reported on return stays in Rental.** Rental checks the bike and publishes the event, and Incident Handling subscribes to it without Rental knowing. That's a clean boundary, and nothing about it needs to change.

**Account blocking is now a single-responsibility violation.** Two contexts block accounts: Onboarding, when a payment isn't verified in time, and Payments & Debt Recovery, when a payment stays unsettled. Both answer the same question: "is this account blocked, and why?" Heuristic 3 says that answer must live in one place.

Merging Onboarding with Debt Recovery would contradict the pivotal event, User Account Activated, so I'd resolve it differently. I'd move ownership of account status into the onboarding context and widen it into **Rider Accounts**, which owns the whole account lifecycle: registered, activated, blocked, and the reason for the block. Debt Recovery would then only publish something like "Debt Recovery Started", and Rider Accounts would react by blocking the account and sending the notification. Rental's eligibility check would ask a single context, Rider Accounts, instead of two.

## Current map

1. **Rider Accounts:** registration, activation, account status, blocking for either reason, and block notifications.
2. **Rental:** eligibility check, reservation, unlock, session, return to a station or zone, forced end, and fault checks on return.
3. **Pricing:** a stateless fee calculator for the free period, hourly tiers, penalties and zone fees.
4. **Payments & Debt Recovery:** starting the payment after a return, payment failures, and the recovery process.
5. **Fleet Maintenance:** bike condition, maintenance, repairs.
6. **Incident Handling:** incidents reported by riders or detected automatically, through to their resolution.

One question decides whether this holds. Does moving blocking into Rider Accounts match how your business works? The test is whether one owner decides "this rider can't use the service right now" whatever the reason. Or do the payment people treat a debt block as their own decision, with its own rules for when it's lifted?

===

Payment knows nothing about any rules

===

That confirms it: Rider Accounts owns the blocking rules. Payments only executes charges and publishes facts, such as Payment Failed or Payment Not Settled. Rider Accounts listens for those facts, applies its rules, and blocks the account whatever the reason. "Is this rider allowed to use the service?" is now answered in exactly one place, and Rental's eligibility check asks only Rider Accounts.

So the map stands at six contexts:

1. **Rider Accounts:** registration, activation, account status, and all blocking rules and notifications.
2. **Rental:** eligibility check, reservation, unlock, session, returns, forced end, and fault checks on return.
3. **Pricing:** a stateless fee calculator used by Rental and Payments.
4. **Payments & Debt Recovery:** starting payments, reporting their outcomes, and running the recovery process.
5. **Fleet Maintenance:** bike condition and repairs.
6. **Incident Handling:** incidents from reports or detection through to their resolution.

One thing to check against your last answer. If Payments knows no rules, the debt recovery process sits oddly inside it, because pausing, resuming and deciding when recovery fails sounds like rule-driven work. Does debt recovery have its own rules, for example retry schedules or escalation? If it does, heuristic 3 suggests splitting it into its own context, separate from a purely mechanical Payments context. If it simply retries charges until told to stop, it can stay where it is.

===

Debt recovery has its own rules regarding what actions to do, and the schedule, too.

===

Then Debt Recovery is its own context. It answers its own question, "how do we get this money back, and when?", with its own rules for which actions to take and on what schedule. Payments stays purely mechanical: it charges and reports what happened.

## Final map: seven bounded contexts

1. **Rider Accounts:** registration, activation, account status, and all blocking rules and notifications.
2. **Rental:** eligibility check, reservation, unlock, session, returns to a station or zone, forced end, and fault checks on return.
3. **Pricing:** a stateless fee calculator used for both the live estimate and the final charge.
4. **Payments:** charges started after a ride ends, and publishing their outcomes. It holds no business rules.
5. **Debt Recovery:** the recovery process, its actions and schedule, and its outcomes (succeeded, failed, paused, resumed).
6. **Fleet Maintenance:** bike condition, maintenance and repairs, independent of rentals.
7. **Incident Handling:** incidents reported by riders or detected automatically, and their lifecycle.

Checking the final set against heuristic 3, each context now answers a different question and nothing is duplicated:

- Rider Accounts: "Can this rider use the service?"
- Rental: "Who has which bike, and when?"
- Pricing: "What does this cost?"
- Payments: "Did the charge go through?"
- Debt Recovery: "How do we recover what's owed?"
- Fleet Maintenance: "Is this bike in working order?"
- Incident Handling: "What went wrong, and is it resolved?"

The communication between them follows from your answers:

- Rental publishes "ride ended" and calls Pricing for the total.
- Payments charges the rider and publishes the result.
- Rider Accounts and Debt Recovery both react to a failed payment: Rider Accounts blocks the account, and Debt Recovery starts its process. Recovery may in turn ask Payments to retry a charge.
- Incident Handling quietly subscribes to fault events from Rental.

It's worth updating the board to match. Add the missing events (Session Forcibly Ended, Payment Succeeded, Payment Failed), and rename the hourly "Fee Applied" stickies to something like "Fee Estimated" so nobody reads them as charges. I can also draw this as a context map with the event flows between contexts if that would be useful for your session write-up.

===

So, who actually makes the settlement here?

===

Nobody does, and that's a gap in my map. I glossed over it when I wrote that Rental "calls Pricing for the total" and Payments "charges". Neither of those is settlement. Pricing is a calculator with no state. Payments has no rules. Rental's job ends when the ride ends.

That leaves an unanswered question: "what does the rider owe for this rental, and is it paid?" Someone has to build the final bill from the ride facts (duration, penalty, zone fees), start the payment, track whether it settles, and decide when a payment counts as unsettled. That last decision is a rule, so by your own definition Payments can't own it. Yet Rider Accounts and Debt Recovery both react to exactly that decision.

Heuristic 1 points the same way. Bike returned / session ended is a confirmed pivotal event, and I had the ride phase on one side of it and nothing on the other. What happens after the pivot is settlement, so it should be a context of its own, something like **Billing** or **Settlement**:

- It reacts to Rental's "session ended" event, which carries the ride facts.
- It asks Pricing for the amount.
- It issues the bill and tells Payments to charge.
- It interprets the result and publishes Rental Settled, or Payment Unsettled once its deadline rules say so.

Rider Accounts and Debt Recovery then react to Billing's events, not to raw payment results.

That would make eight contexts, with Rental ending cleanly at the pivot. Does settlement actually carry rules of its own, such as payment deadlines, retries before something counts as unsettled, or how fees are combined into one bill? If it does, it's a separate context. If it's purely "calculate and send to Payments", it could fold back into Rental.

===

settlemen has its own rules, not very complex though (mostly about schedule). Still, independent from the rental itself.

===

Then Settlement is its own context. It has independent rules, and it sits on the far side of a confirmed pivotal event. The rules being simple doesn't change the boundary. It only means Settlement is probably a supporting context that doesn't need heavy modeling.

## Final map: eight bounded contexts

1. **Rider Accounts:** registration, activation, account status, and all blocking rules and notifications.
2. **Rental:** eligibility check, reservation, unlock, session, returns, forced end, and fault checks on return. It ends at the pivot, the end of the session.
3. **Pricing:** a stateless fee calculator used for the live estimate and the final bill.
4. **Settlement:** builds the bill from the ride facts, starts the payment, and applies its schedule rules to decide whether the rental is settled or unsettled.
5. **Payments:** executes charges and reports the outcomes, with no business rules.
6. **Debt Recovery:** its own actions and schedule for recovering unsettled amounts.
7. **Fleet Maintenance:** bike condition and repairs.
8. **Incident Handling:** incidents and their lifecycle.

The main flow now reads cleanly:

1. Rental publishes Rental Session Ended, carrying the ride facts.
2. Settlement gets the amount from Pricing and asks Payments to charge.
3. Settlement interprets the result according to its schedule and publishes either Rental Settled or Rental Unsettled.
4. On Rental Unsettled, Rider Accounts blocks the account and Debt Recovery starts its process.
5. Rider Accounts also blocks accounts when a payment method isn't verified in time, which is its own onboarding rule.

Each of the three pivotal events now marks a real context boundary. User Account Activated separates Rider Accounts from Rental. Rental Session Started turned out to be a phase change inside Rental, not a boundary. And the end of the session separates Rental from Settlement.

To bring the board in line with this, I'd add these events: Rental Session Ended (including a forced end), Payment Succeeded/Failed, and Rental Settled/Unsettled. I'd also rename the hourly "Fee Applied" stickies to something like "Fee Estimated".
