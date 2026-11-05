# Bounded context discovery: VeloCity (validation of the reference context map)

- **Date:** 2026-11-05
- **Mode:** validate
- **Status:** map proposed, awaiting the trainer's approval (the original CML is unchanged)
- **Input:** src/main/cml/VeloCity-Context-Map.cml (the reference solution), velocity/velocity-context-map.md, velocity/velocity-vision.md, velocity/bounded-context-discovery-session.md
- **Context map:** reviewed src/main/cml/VeloCity-Context-Map.cml (not modified). The corrected proposal is in **src/main/cml/VeloCity-Context-Map-validated.cml**.

## Verdict in one paragraph
The reference map parses (0 errors), and its eight contexts hold up under H1, H2 and H3. As written, though, it breaks several of our context-mapping rules:
1. **Eight Conformists with no justification** (integration rule 1).
2. **Two downstreams with neither an ACL nor a CF:** Accounts→Rentals and CustomerSupport→FleetMaintenance (integration rule 3).
3. **An undocumented indirect U/D cycle,** Accounts→Rentals→Settlement→Accounts, while the write-up claims "No cycles" (relationship rule 3).
4. **An unconfirmed relationship drawn in the wrong direction.** Support→Fleet was confirmed by the expert, but Fleet Maintenance is upstream through a handover OHS.
5. **Confirmed hand-offs with no relationship:** billing disputes to Settlement and Debt Recovery, and rider contact details to Debt Recovery.
6. **Misleading ownership wording** about who blocks accounts and who withdraws bikes.

None of these is a Shared Kernel, a Partnership, a Big Ball of Mud or a Separate Ways. All of them are fixed in the proposed file.

## Reading of the input
- The map has 8 internal contexts (Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport, FleetMaintenance) and 2 external parties (PaymentProcessor, City).
- It has 11 relationships. One of them, CustomerSupport→FleetMaintenance, is marked unconfirmed.
- The facilitator tested the map against the vision's flows. The expert re-cut them into **four processes**:
  1. Onboarding and activation
  2. Rent and return a bike
  3. Settle a ride and recover the debt, including billing disputes from Support
  4. Bike fault to maintenance, including fleet-issue incidents from Support

  "Invalid card → block" is a test scenario, not a process.
- The user is the trainer and the author of the reference solution, so the workshop-spoiler rule doesn't apply.

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| User Account Activated | confirmed (discovery session) | Accounts | Rentals | unchanged |
| Rental Session Started | confirmed phase change, not a boundary | Rentals | Rentals | unchanged (critic: no straddle) |
| Rental Session Ended | confirmed | Rentals | Settlement | "Rental session ended, with start, end, location and reason" is shared company language |
| Rental Fee Unpaid | confirmed hand-off | Settlement | Accounts (block), DebtRecovery (start) | "once the automatic retries are used up" |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Maintenance | lane | independent, follows the bike (critic, round 1) |
| Debt recovery | lane | its own schedule. For the expert it is a stage of one end-to-end process with settlement, but it is still a separate context because it has its own rules |
| Incidents | lane, but not a value stream | "Support takes in what riders tell us and feeds it into those processes" |
| Technician vs rider reserve/unlock | variant | "exactly the same way and with exactly the same words as riders" |
| No live price estimate | n/a | "Riders don't see a running price estimate during a ride" |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Account block/unblock | Write-up: "Settlement drives both block and unblock". Discovery session: Accounts owns all blocks | Expert: Settlement states the facts, and "Nobody else blocks or unblocks an account directly". Accounts is the sole owner. The CML direction is right, the wording is wrong |
| "Bike is broken / off offer" | Rentals (withdraws on faults it detects), Support (accepts fleet issues), FM ("Broken bikes") | Rentals withdraws immediately on faults seen in its own flow: lock failure, a fault reported at return, the bike's own signal. Support decides "broken" for faults reported through support. FM alone puts a bike back: "The decision to take the bike out and the decision to put it back stay with different people." |
| Dispute decision | Support agents and the "specialised billing team" | Expert: that team is part of Support, so Support is the single owner |
| Moving money | Settlement, and possibly DebtRecovery or Support | "Only the balance people move money, in both directions." Settlement cancels and refunds |

## Bounded contexts
No boundary changes. Only the ownership wording is sharpened in the proposal.

| Context | Question it answers | Owns | Heuristic justification |
|---|---|---|---|
| Accounts | Can this rider use the service? | onboarding, activation, every block and unblock, contact details | H1 activation pivot; H3 single owner of blocks |
| Rentals | Who has which bike, and when? | availability (available/withdrawn), reserve/unlock for riders and technicians, sessions, returns, max-time end, immediate withdrawal on faults in its own flow | H1 (session start is internal, session end is the boundary); H3 |
| Pricing | What does this ride cost? | total fee, tariffs, zone fees | H3: changes independently |
| Settlement | What does the rider owe, and is it settled? | charging, retries, balance, debt, cancellations, refunds | H1 far side of session end; H3 only mover of money |
| Payments | Did the verification or charge go through? | verification, charge, shielding from the processor | no rules (discovery session) |
| DebtRecovery | How do we recover what's owed? | reminders, payment plans, pause/resume | H2 lane with its own rules |
| CustomerSupport | What went wrong for the rider, and is it resolved? | incidents, confirming fleet issues, deciding disputes | H2 lane; H3 single dispute owner |
| FleetMaintenance | Is this broken bike fixed, and when is it back in circulation? | maintenance queue, maintenance, fixed, make available again | H2 lane; H3 sole owner of return to circulation |

Context critics ran only for the flagged contexts, Rentals and FleetMaintenance, which split the "broken/withdrawn" decision. No other context was flagged.

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Rentals | 1 | DISAGREE | A fault reported at return is withdrawn on the rider's word, while support reports need acceptance. Asked the expert (round 4) |
| FleetMaintenance | 1 | DISAGREE | The CML says FM owns "broken", but the expert gave that decision to Support. Same question about faults at return and device signals |
| Rentals | 2 | AGREE | Fix (a): Rentals withdraws on faults in its own flow and never decides broken or fixed |
| FleetMaintenance | 2 | AGREE | Restated as "fixed / back in circulation", taking in facts from Rentals and Support |
| Map, all 4 processes | 1 | DISAGREE | The Support→FM direction can't give "no window" or a confirmation back. City CF not confirmed. Questions on partial and paid disputes and on reminder contacts |
| Map, all 4 processes | 2 | AGREE | FM→Support handover OHS, City CF justified, refunds and partial cancellations, Accounts→DebtRecovery contacts |

## Context map
### Findings on the ORIGINAL map (src/main/cml/VeloCity-Context-Map.cml)
| # | Finding | Rule | Fix in the proposal |
|---|---|---|---|
| 1 | 8 Conformists with no justification in the CML or the write-up | integration rule 1 ("justify every CF") | The expert justified each one: one company, and the terms are used identically and have been stable since launch. A comment on each. The City CF is justified by its fixed, documented external format |
| 2 | `Accounts [U]->[D] Rentals` has no downstream pattern | integration rule 3 | `Accounts [U,OHS]->[D,CF] Rentals`: Rentals takes "active" as it is |
| 3 | `CustomerSupport [U]->[D] FleetMaintenance` is unconfirmed, has no pattern, and points the wrong way | integration rule 3; direction follows model dependency | Confirmed. Flipped to `FleetMaintenance [U,OHS,PL]->[D,ACL] CustomerSupport : BrokenBikeHandover`: the handover uses FM's terms (bike, fault), FM confirms back, and the withdrawal happens inside the call, so there is no window |
| 4 | Indirect cycle Accounts→Rentals→Settlement→Accounts, while the write-up says "No cycles" | relationship rule 3; "Breaking a cycle" | Kept and documented. No fact-consistent flip exists (see below). Fix the write-up's claim |
| 5 | No relationship for billing disputes (pause/resume, cancel, refund), so process 3 can't run | cross-cutting: every hand-off needs a relationship | `Settlement [U,OHS,PL]->[D,ACL] CustomerSupport : DisputedAmountCancellation` and `DebtRecovery [U,OHS,PL]->[D,ACL] CustomerSupport : RecoveryPause` |
| 6 | No relationship for reminder contact details | same | `Accounts [U,OHS]->[D,ACL] DebtRecovery : RiderContactDetails` |
| 7 | Write-up: "Settlement drives both block and unblock for debt" | H3 wording | Reword: Settlement states Rental Fee Unpaid and Outstanding Debt Cleared (paid or cancelled), and Accounts decides |
| 8 | "reserve & unlock for any authorised client" and "Broken bikes" in FM | H3 wording | Reworded, see the contexts |
| 9 | Accepted alternatives "Debt Recovery → Accounts for unblocking" and "Fleet Maintenance upstream of Rentals" contradict this expert's facts | n/a | They can stay as debrief alternatives for groups whose expert answers differently. Note this in the debrief |

Not violated: no Shared Kernel, Partnership, Separate Ways or Big Ball of Mud. OHS and PL are only upstream. The ACL towards the PaymentProcessor is correct.

### Breaking the indirect cycle (tried, none fact-consistent)
| Flip | Ruled out by |
|---|---|
| Settlement→Accounts: Accounts offers a block/unblock OHS that Settlement calls | "Nobody else blocks or unblocks an account directly" (reply 1.4) |
| Accounts→Rentals: Rentals defines eligibility | "'Active' means activated and not blocked, exactly as the accounts team defines it, and the rental side takes that answer as it is" (reply 2.4) |
| Rentals→Settlement: Settlement offers a "settle ride" OHS | "Rental session ended, with start, end, location and reason" is shared language adopted as is (reply 1.5); Rentals ends at the pivot |
| Move a responsibility | Each link carries a decision its upstream owns |

What remains is a genuine business feedback loop: an unpaid ride blocks future rentals.

### Process walkthroughs (proposed map)
#### 1. Onboarding and activation, plus the invalid card scenario
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| register … sign agreement, activate | Accounts | – |
| verify payment method | Payments | PaymentMethodVerification (Payments OHS/PL → Accounts CF) |
| Payment Method Invalidated → block, notify | Payments → Accounts | PaymentMethodVerification |
#### 2. Rent and return a bike
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| can this person rent now? | Accounts | AccountStatus (OHS → CF) |
| service area, reserve, unlock, session, return, lock, max time | Rentals | – |
| fault at return, lock failure, bike signal → withdraw | Rentals | Bike Withdrawn → FM via BikeAvailability |
| Rental Session Ended (start, end, location, reason) | Rentals | RentalSessionEnded (PL → CF) |
#### 3. Settle a ride and recover the debt
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| price (tariff at start, zone fees) | Pricing (zone map from City) | PriceCalculation; ZoneMap |
| charge, retries, refund | Settlement via Payments | Charging; PaymentProcessing |
| Rental Fee Unpaid → block | Settlement → Accounts | DebtBlocking |
| Rental Fee Unpaid → start recovery, attempt collection | Settlement → DebtRecovery | DebtCollection |
| reminders using current contacts | DebtRecovery | RiderContactDetails |
| dispute opened → pause; rejected or partly upheld → resume | CustomerSupport → DebtRecovery | RecoveryPause |
| upheld → cancel (full or part) / refund paid charge | CustomerSupport → Settlement | DisputedAmountCancellation |
| Outstanding Debt Cleared (paid / cancelled) → unblock, close case | Settlement | DebtBlocking, DebtCollection |
#### 4. Bike fault to maintenance
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| fault in Rentals' flow → withdrawn | Rentals → FM | BikeAvailability |
| fleet issue accepted → hand over broken bike, confirmation, incident resolved | CustomerSupport → FM | BrokenBikeHandover (FM OHS; withdraw via Rentals inside the call) |
| technician reserve, unlock → Maintenance Started; fixed → make available | FM via Rentals | BikeAvailability |

### Relationships (proposed)
| Upstream | Downstream | Patterns | Messages | Justification |
|---|---|---|---|---|
| PaymentProcessor | Payments | U / ACL | processing | external; ACL default |
| City | Pricing | U / CF | zone map | CF: City's fixed, documented format (reply 5.2) |
| Payments | Accounts | OHS,PL / CF | verify, invalidated | CF: shared language (reply 1.5) |
| Payments | Settlement | OHS,PL / CF | charge, refund | same |
| Accounts | Rentals | OHS / CF | can rent now | reply 2.4 |
| Accounts | DebtRecovery | OHS / ACL | contact details | reply 5.4; ACL default |
| Rentals | Settlement | PL / CF | Rental Session Ended | reply 1.5 |
| Rentals | FleetMaintenance | OHS,PL / CF | Bike Withdrawn, withdraw/make available, reserve/unlock | reply 2.3 |
| Pricing | Settlement | OHS / CF | price for session | reply 1.5 |
| Settlement | Accounts | PL / CF | Rental Fee Unpaid, Outstanding Debt Cleared | reply 1.4/1.5 |
| Settlement | DebtRecovery | OHS,PL / CF | start recovery, attempt collection, cleared | reply 2.4 |
| Settlement | CustomerSupport | OHS,PL / ACL | cancel (full/part), refund | replies 2.1, 5.3; ACL default |
| DebtRecovery | CustomerSupport | OHS,PL / ACL | pause, resume | replies 1.3, 2.1, 5.3; ACL default |
| FleetMaintenance | CustomerSupport | OHS,PL / ACL | hand over broken bike, confirmation | replies 4.3, 5.1; ACL default |

### Validator output
Original (`src/main/cml/VeloCity-Context-Map.cml`):
```
RELATIONSHIPS:
  PaymentProcessor [] -> Payments [ACL] : PaymentProcessing
  City [] -> Pricing [CF] : ZoneMap
RULE conformist-check: Pricing conforms to City. OK only if City's language is well established; otherwise it pollutes Pricing.
  Payments [OHS, PL] -> Accounts [CF] : PaymentMethodVerification
RULE conformist-check: Accounts conforms to Payments. OK only if Payments's language is well established; otherwise it pollutes Accounts.
  Payments [OHS, PL] -> Settlement [CF] : Charging
RULE conformist-check: Settlement conforms to Payments. OK only if Payments's language is well established; otherwise it pollutes Settlement.
  Accounts [] -> Rentals [] : AccountStatus
RULE no-acl: Rentals is downstream of Accounts without an ACL. An ACL is almost always a good idea downstream.
  Rentals [PL] -> Settlement [CF] : RentalSessionEnded
RULE conformist-check: Settlement conforms to Rentals. OK only if Rentals's language is well established; otherwise it pollutes Settlement.
  Rentals [OHS, PL] -> FleetMaintenance [CF] : BikeAvailability
RULE conformist-check: FleetMaintenance conforms to Rentals. OK only if Rentals's language is well established; otherwise it pollutes FleetMaintenance.
  Pricing [OHS] -> Settlement [CF] : PriceCalculation
RULE conformist-check: Settlement conforms to Pricing. OK only if Pricing's language is well established; otherwise it pollutes Settlement.
  Settlement [PL] -> Accounts [CF] : DebtBlocking
RULE conformist-check: Accounts conforms to Settlement. OK only if Settlement's language is well established; otherwise it pollutes Accounts.
  Settlement [OHS, PL] -> DebtRecovery [CF] : DebtCollection
RULE conformist-check: DebtRecovery conforms to Settlement. OK only if Settlement's language is well established; otherwise it pollutes DebtRecovery.
  CustomerSupport [] -> FleetMaintenance [] : FleetIssueIncident_Unconfirmed
RULE no-acl: FleetMaintenance is downstream of CustomerSupport without an ACL. An ACL is almost always a good idea downstream.
RULE ud-cycle-indirect: Accounts -> Rentals -> Settlement -> Accounts. Check whether each context really depends on the next one's model, or whether one arrow is only event flow drawn the wrong way.
VALID: 10 bounded contexts, 11 relationships, 0 errors
```
Proposed (`src/main/cml/VeloCity-Context-Map-validated.cml`):
```
RELATIONSHIPS:
  PaymentProcessor [] -> Payments [ACL] : PaymentProcessing
  City [] -> Pricing [CF] : ZoneMap
RULE conformist-check: Pricing conforms to City. OK only if City's language is well established; otherwise it pollutes Pricing.
  Payments [OHS, PL] -> Accounts [CF] : PaymentMethodVerification
RULE conformist-check: Accounts conforms to Payments. OK only if Payments's language is well established; otherwise it pollutes Accounts.
  Payments [OHS, PL] -> Settlement [CF] : Charging
RULE conformist-check: Settlement conforms to Payments. OK only if Payments's language is well established; otherwise it pollutes Settlement.
  Accounts [OHS] -> Rentals [CF] : AccountStatus
RULE conformist-check: Rentals conforms to Accounts. OK only if Accounts's language is well established; otherwise it pollutes Rentals.
  Accounts [OHS] -> DebtRecovery [ACL] : RiderContactDetails
  Rentals [PL] -> Settlement [CF] : RentalSessionEnded
RULE conformist-check: Settlement conforms to Rentals. OK only if Rentals's language is well established; otherwise it pollutes Settlement.
  Rentals [OHS, PL] -> FleetMaintenance [CF] : BikeAvailability
RULE conformist-check: FleetMaintenance conforms to Rentals. OK only if Rentals's language is well established; otherwise it pollutes FleetMaintenance.
  Pricing [OHS] -> Settlement [CF] : PriceCalculation
RULE conformist-check: Settlement conforms to Pricing. OK only if Pricing's language is well established; otherwise it pollutes Settlement.
  Settlement [PL] -> Accounts [CF] : DebtBlocking
RULE conformist-check: Accounts conforms to Settlement. OK only if Settlement's language is well established; otherwise it pollutes Accounts.
  Settlement [OHS, PL] -> DebtRecovery [CF] : DebtCollection
RULE conformist-check: DebtRecovery conforms to Settlement. OK only if Settlement's language is well established; otherwise it pollutes DebtRecovery.
  Settlement [OHS, PL] -> CustomerSupport [ACL] : DisputedAmountCancellation
  DebtRecovery [OHS, PL] -> CustomerSupport [ACL] : RecoveryPause
  FleetMaintenance [OHS, PL] -> CustomerSupport [ACL] : BrokenBikeHandover
RULE ud-cycle-indirect: Accounts -> Rentals -> Settlement -> Accounts. Check whether each context really depends on the next one's model, or whether one arrow is only event flow drawn the wrong way.
VALID: 10 bounded contexts, 14 relationships, 0 errors
```
How each RULE line in the proposal was resolved:
- **9 × conformist-check:** each is justified in a CML comment by the expert's answers (replies 1.5, 2.3, 2.4 and 5.2).
- **ud-cycle-indirect:** kept. Every flip was tried and is ruled out by a stated fact (see the table above).

## Q&A with the domain expert
The full verbatim Q&A, with five rounds, is in the session transcript. It is reproduced here.

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

## Open questions and assumptions
- **ACL by default** on four links: Settlement→Support, DebtRecovery→Support, FM→Support and Accounts→DebtRecovery. This is the rule default; the expert wasn't asked about translation on these links.
- **Who triggers the resume after a partial uphold.** The map has Support doing it via RecoveryPause, which is consistent with reply 2.1 but not stated explicitly. Either answer keeps the same relationships.
- **Where Rentals learns who is a technician.** Not discussed.
- **Future:** long-term fleet distribution will need the City's zone map (reply 5.2). Not at launch.

## Suggested board changes
- **Add these events:**
  - Bike Withdrawn, with a reason: lock failed, fault reported at return, or bike fault signal
  - Bike Made Available
  - Technician Unlocked Bike, which triggers Maintenance Started
  - Broken Bike Handed Over, with a confirmation
  - Outstanding Debt Cleared (paid / cancelled)
  - Dispute Opened / Rejected / Upheld (full / partial)
  - Charge Refunded
- **Carry the lock-failure reason** on Rental Session Ended, so that Pricing or Settlement can apply the "not charged extra" rule.
- **Write-up velocity-context-map.md:** fix "No cycles", reword "Settlement drives both block and unblock", and move the fleet-incident and billing-dispute items out of "Deferred / Open".

## Next steps
- The trainer reviews the proposal. Only on the trainer's request does it replace the reference CML; if it does, also sync velocity-context-map.md and VeloCity-Bounded-Contexts.cml.
- Canvases via bounded-context-scaffolder were **not** generated, because this was a validation. They are offered once the map is approved.
