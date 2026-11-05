# Transcript: VeloCity reference map validation (2026-11-05)

## User (initial request)

"can you sanity-check the VeloCity context map in src/main/cml/VeloCity-Context-Map.cml? I wrote it as the reference solution for the workshop and I want to know if it breaks any of our context mapping rules before I show it to the group."

## Facilitator -> user, message 1

I'm treating this as a **validation** of your reference map: I won't touch `src/main/cml/VeloCity-Context-Map.cml`. Any fix goes into a separate proposal file.

**How I read it.** 8 contexts + 2 externals (PaymentProcessor, City), 11 relationships. I tested it against the processes in `velocity-vision.md`: onboarding, rent a bike, return and settle, invalid card block, debt recovery, broken bike maintenance, incident handling. The CML parses cleanly (0 errors). The validator raised these rule smells:

1. **Indirect U/D cycle: Accounts -> Rentals -> Settlement -> Accounts.** Rentals asks Accounts "is the account active?", Settlement reacts to Rental Session Ended, and Accounts reacts to Settlement's Rental Fee Unpaid / Outstanding Debt Cleared. Your `velocity-context-map.md` says "No cycles", so the map and the doc disagree here.
2. **Conformist on 8 of 11 relationships**, 7 of them between your own contexts (e.g. Accounts conforms to Settlement's debt language, Settlement conforms to Pricing and Payments). Our rule: CF only towards a well-established language, ACL by default downstream.
3. **Accounts -> Rentals has no pattern on either side** (no OHS, no ACL/CF).
4. **No Customer/Supplier anywhere**, although it's our default flavour of U/D between internal teams.
5. **CustomerSupport -> FleetMaintenance is still marked unconfirmed**, and it's Customer Support's only relationship.

Before I call any of these a finding, I need a few business facts:

1. **Blocking for debt (decides the cycle).** When a rider's fee stays unpaid after the retries, who decides that the account is blocked, and who decides it's unblocked once the money arrives? What does Accounts actually need to know at that moment?
2. **Teams (decides C/S and CF vs ACL).** Who builds these contexts: separate teams inside VeloCity, or is any of them bought or run by another company? When a downstream team needs a change in an upstream model, does the upstream team plan for it?
3. **The City's zone map (decides City -> Pricing CF).** What does the city actually hand over: a standard format it publishes for every operator, or something agreed just with VeloCity?
4. **Fleet-issue incidents (decides the unconfirmed arrow).** When support accepts an incident like "broken brake", what happens to the bike next, and who does it?
5. **Billing disputes.** When an incident is a billing dispute about a ride whose debt is being recovered, does anything happen to the recovery or the charge, and who triggers it?

## Expert reply 1

1. **Blocking for debt.** Nobody decides it case by case. It's a fixed rule: if a rider has outstanding debt, the account is blocked, and when the debt is cleared the block lifts. The debt is decided on the money side. After the quick retries fail, the people who keep the rider's balance record the unpaid amount as outstanding debt. That same fact is what clears it later: once the balance goes back to zero, the debt is cleared. It doesn't matter whether the money came from the rider paying, from a payment plan, or from a later charge going through. The debt recovery people chase the money, but they don't say "unblock". The unblock follows from the balance being settled. Accounts only needs to know two things: "this rider now has outstanding debt" and later "this rider's outstanding debt is cleared". It doesn't need the amount, the ride, or anything about how the money was chased. It needs the reason only so it can keep a debt block apart from an invalid-card block and send the right block notification.

2. **Teams.** Every one of these is built and run by VeloCity's own teams, all in one company. Nothing is bought, and no outside company runs any of it. The only outsiders are the payment processor and the city. Inside the company we plan together. When one team needs something changed in what another team provides, the providing team puts it in its own planning and agrees a date with them. We don't make teams live with whatever they're given.

3. **The city's zone map.** The city publishes a single zone map, in its own format, for every licensed bike-share operator. It shows stations, drop zones and the service area. VeloCity has no say in the format or the content. We take what the city publishes and use it as is. Nothing about it was agreed with us in particular.

4. **Fleet-issue incidents.** When the support agent accepts a fleet-issue incident like "broken brake", the agent reports that bike as broken. From then on it isn't offered to riders, and the technicians take it over through the normal maintenance flow. That flow is: a technician reserves the bike, unlocks it, starts maintenance, fixes it, and puts it back into circulation. The agent doesn't wait for the repair. Once the bike has been handed to the technicians, the agent can resolve the incident.

5. **Billing disputes.** Yes, something happens. When the agent accepts a billing dispute about a ride whose debt is being recovered, the agent pauses the debt recovery while the dispute is under review. Then:
   - If the rider turns out to be right, the charge for that ride is corrected or cancelled, so the outstanding debt goes down or disappears. If the debt is fully gone, recovery ends and the block lifts as usual.
   - If the rider turns out to be wrong, the agent lifts the pause and recovery resumes.
   The agent resolves the incident either way.

## Facilitator -> user, message 2

Thanks. Here's what your answers settle, and what they move.

**Settled**
- **CF between your own contexts isn't justified.** All teams are internal and the provider plans for the consumer, so Customer/Supplier is the right flavour, with an ACL downstream instead of CF. That turns 7 of the 8 CFs into findings.
- **City -> Pricing CF is fine.** The city publishes one format for every operator and you take it as is. That's exactly the "well-established language" case.
- **Debt and its clearing are Settlement's facts**, not Debt Recovery's. The map has that right.

**Moved**
- **Customer Support -> Fleet Maintenance is probably drawn the wrong way.** The agent *reports* a bike as broken, which is a request into someone else's model. The side that receives the request and defines it is upstream, not the sender.
- **Billing disputes need relationships the map doesn't have yet.** The agent pauses and resumes Debt Recovery, and a charge gets corrected or cancelled.

**Still open: the cycle.** Each arrow matches something you said: Rentals needs Accounts' answer before a ride, Settlement needs the ride's end, and Accounts needs to hear "outstanding debt" / "debt cleared". So no flip fits your facts automatically. In business terms the options are:
- **A.** Accounts offers a "place / lift a block, with this reason" service. The money side calls it when debt appears and when it clears. Accounts then knows block reasons, not debt.
- **B.** Before a ride, Rentals asks the money side about debt directly. Accounts handles only onboarding and invalid-card blocks, and debt blocks and their notification move out of Accounts.
- **C.** Keep it as it is: Accounts listens to the money side's "outstanding debt" / "debt cleared", and the three-way loop stays, recorded and explained.

Questions:
1. **Which of A, B or C describes how VeloCity actually works,** or does none of them? If you'd keep C, say so explicitly. (Decides whether the cycle stays.)
2. **Where does the agent report a broken bike?** Does it go to the technicians' side, or to the side that offers bikes to riders, which then tells the technicians? (Decides the direction and target of the fleet-incident arrow.)
3. **Who actually corrects or cancels the disputed charge?** Does the agent change it, or does the agent ask the people who keep the rider's balance to do it? Does the agent need anything back before resolving the incident? (Decides the Settlement and Customer Support relationship.)

## Expert reply 2

1. **A describes how we work.** The block belongs to the account. The account side places it, keeps the reason, and sends the block notification that tells the rider what happened and what to do next. Whether a rider owes money, and when that's over, is decided on the money side. When an unpaid amount becomes outstanding debt, the money side asks for a block with the reason "outstanding debt". When the balance is back to zero, it asks for that block to be lifted. The account side doesn't track debt, amounts or rides. It only knows the account is blocked, why, and when that reason is cleared. Before a ride, the only question asked is still "is this account active?", and the answer comes from the account side. So B isn't how we work, and I wouldn't keep C.

2. **The agent reports the broken bike to the technicians' side.** In our process, the bike is marked broken and handed over to technicians, and the agent deals only with them. From then on the technicians' side makes sure the bike is no longer offered to riders and takes it through the usual maintenance steps. The agent never deals directly with the side that offers bikes to riders.

3. **The agent asks the people who keep the rider's balance to correct or cancel the charge.** The agent never changes the charge or the balance directly. The agent needs a confirmation back that the correction was made, with the rider's new outstanding amount, before resolving the incident. If that amount is zero, the debt is cleared and the block lifts as usual, without the agent doing anything more. If something is still owed, the agent lifts the pause and recovery resumes for what is left. If the rider was wrong, no correction is requested: the agent lifts the pause and resolves the incident.

## Critic verdicts (round 1)

- Accounts, round 1: AGREE. Single owner of blocks/reasons/notification; Settlement owns only the debt fact and the trigger; note to track blocks per reason.
- Settlement, round 1: AGREE. 'Charging' duplication resolved (decide vs execute); corrections belong here; non-blocking question on refunds for already-paid rides.
- Payments, round 1: AGREE. Purely mechanical but a real context with two consumers; suggests clearer 'charge execution' naming.
- Map (all processes P1-P7), round 1: DISAGREE. All five findings confirmed, no cycles in the proposal; but P2 'location validated' (service area) has no owner or relationship in either map, so ask the expert.

## Facilitator -> user, message 3

Thanks, option A removes the cycle. The corrected map now has no cycles, and the critics agree with Accounts, Settlement and Payments as drawn. The final check across all seven processes found one step that neither your map nor my proposal gives an owner. I also have one smaller question:

1. **Location check before a ride.** Before a rider can reserve a bike, their location is checked: they must be inside the service area. Who does that check, and where does the service area they check against come from? (Decides whether the city's zone map feeds anything besides pricing.)
2. **Disputes about rides already paid.** If a billing dispute is about a ride that was already charged successfully and the rider turns out to be right, does the money go back to their card? If so, who makes that happen? (Decides whether refunds travel the same way as charges.)

## Expert reply 3

1. **Location check.** The check is the first part of reserving a bike, and whoever handles reserving and unlocking bikes does it. When the rider taps a bike, the app sends their current location. Before the reservation is made, it is checked for two things: the rider must be inside the service area, and they must be standing near that bike. If either check fails, there is no reservation. We trust the location the app sends. The service area isn't something VeloCity draws itself. It is part of the zone map the city publishes for every operator, the same one that shows stations and drop zones, in the city's format, used as is. So the same city map is used twice: to decide whether a ride may start (service area) and to decide what a return costs (station, drop zone or unauthorized zone).

2. **Disputes about rides already paid.** Yes, the money goes back to the rider's card. The agent never moves money. When the rider turns out to be right, the agent asks the people who keep the rider's balance to correct the charge. They work out the amount to give back and refund it to the card through the payment processor, the same way the money was taken. The agent needs a confirmation that the refund was issued, with the amount, before resolving the incident. The rider doesn't have to wait for the money to arrive on the card. Nothing about blocks or debt recovery changes, because a ride that was already paid never created debt.

## Critic verdicts (round 2)

- Map (all processes P1-P7), round 2: AGREE. Location check owned by Rentals via City -> Rentals ServiceArea; refunds fit Charging/ChargeCorrection; acyclic; wording notes (refund in key language) applied afterwards.

## Facilitator -> user, final message

Short answer: yes, the reference map breaks several of our rules. The CML parses fine, but I wouldn't show it to the group as is. Your file is untouched. The corrections are in a separate proposal, and all critics agree with it.

**What it breaks**
1. **An indirect cycle: Accounts -> Rentals -> Settlement -> Accounts.** `velocity-context-map.md` says "No cycles". Fix, from your answer (option A): Accounts offers "place / lift a block, with this reason", and Settlement calls it.
2. **7 Conformists between your own teams.** CF is only allowed towards a well-established language. Since every team is internal and plans for its customers, these become Customer/Supplier with an ACL downstream. The City -> Pricing CF is fine.
3. **Accounts -> Rentals** has no pattern on either side.
4. **CustomerSupport -> FleetMaintenance** was unconfirmed and pointed the wrong way. The agent reports a broken bike into Maintenance's model, so FleetMaintenance is upstream. Now confirmed.
5. **Missing relationships for billing disputes:** pausing recovery, and charge correction or refund.
6. **The location check before a reservation had no owner.** Rentals owns it and needs the city's service area, so I added City -> Rentals.
7. **"Charging" was a responsibility of both Payments and Settlement (H3).** Settlement decides, Payments executes.

The proposal has 8 contexts (unchanged), 2 externals and 14 relationships, with no cycles. It validates with 0 errors. The only RULE lines are the two justified City CFs.

**Critic rounds that changed something**
- Map critic, round 1: it found the unowned location check, which led to finding 6.
- Settlement critic: it asked about refunds on already-paid rides, which now go through Charging.

**Files**
- `src/main/cml/VeloCity-Context-Map-validation.cml`
- `docs/context-discovery/velocity/2026-11-05-reference-map-validation/discovery.md`

**Still open**
- Do you approve the proposal? If you do, I'll update `VeloCity-Context-Map.cml`, `VeloCity-Bounded-Contexts.cml` and `velocity-context-map.md`. The doc's "Settlement drives block and unblock" and the "Debt Recovery -> Accounts for unblocking" alternative contradict your answers.
- What lifts an invalid-card block? I haven't asked yet, and it shouldn't move a boundary.
