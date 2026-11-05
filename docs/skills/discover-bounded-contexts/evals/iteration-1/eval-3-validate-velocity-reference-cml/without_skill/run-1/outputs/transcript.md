# Transcript

## User

can you sanity-check the VeloCity context map in src/main/cml/VeloCity-Context-Map.cml? I wrote it as the reference solution for the workshop and I want to know if it breaks any of our context mapping rules before I show it to the group.

## Assistant -> expert (message 1)

I've read src/main/cml/VeloCity-Context-Map.cml and velocity/velocity-context-map.md, and checked that the CML loads and renders with Context Mapper (it does). Before I judge it against "our context mapping rules", I need the rules themselves - they aren't written down anywhere I can see. Could you list them? In particular:

1. Shared Kernel / Partnership - allowed or not?
2. Cycles - is the rule "no upstream/downstream cycles" only about direct pairs (A->B and B->A), or about any cycle through several contexts (A->B->C->A)?
3. ACL and Conformist - must every downstream state ACL or CF explicitly, and are there cases where one is required (e.g. external systems must always be behind an ACL)?
4. OHS / PL - must every upstream declare them, and must they come together (OHS always with PL), or is PL alone / OHS alone fine?
5. Anything else - e.g. are unconfirmed / assumed relationships allowed in a reference solution, rules about relationship naming, external systems, etc.?

Things I already noticed that may matter, depending on your answers:
- Accounts -> Rentals -> Settlement -> Accounts forms a three-context upstream/downstream loop (Accounts [U]->[D] Rentals; Rentals [U,PL]->[D,CF] Settlement; Settlement [U,PL]->[D,CF] Accounts).
- Accounts [U]->[D] Rentals and CustomerSupport [U]->[D] FleetMaintenance have no patterns on either side.
- City [U]->[D,CF] Pricing: Pricing conforms to an external party's model.
- Rentals->Settlement and Settlement->Accounts are PL without OHS; Pricing->Settlement is OHS without PL.
- CustomerSupport -> FleetMaintenance is named FleetIssueIncident_Unconfirmed and is marked as not confirmed with you.

## Expert reply 1

Fair point, they aren't in the repo docs. Here are the rules I use. They override generic DDD advice.

**1. Shared Kernel / Partnership**
- Shared Kernel: no. It's almost always wrong. I'd accept it only with an explicit reason that I agreed to, and nothing in VeloCity has one.
- Partnership: only in early stages. VeloCity's boundaries are established, so prefer Upstream/Downstream. If you propose a Partnership anywhere, say which stage you assume and what would move it to U/D.
- Big Ball of Mud is always wrong. Report it as a finding, never offer it as an option.
- Separate Ways is only for a deliberate R&D silo. Two contexts that simply don't talk just have no relationship.

**2. Cycles**
Both kinds count.
- A direct A<->B pair is almost always wrong.
- A loop through several contexts (A->B->C->A) also has to be checked. For each arrow, ask whether it is a real model dependency or just the direction an event travels. Direction follows dependency on the model, not the direction of a call or an event.
- If every arrow in the loop is a real model dependency, it's a finding. So yes, your Accounts -> Rentals -> Settlement -> Accounts loop needs that check, arrow by arrow. The reference doc claims "no cycles", so tell me whether it holds.

**3. ACL and Conformist**
- Every downstream must state ACL or CF. A downstream with neither is a gap.
- ACL is the default downstream.
- CF is allowed only towards a well-established domain and language, such as a payment provider's standard API or an industry standard. Every CF needs a written justification.
- External systems: there's no hard "always ACL" rule. CF to an external party is fine only if its model is a well-established standard. Otherwise it's an ACL, because an external model we can't influence would pollute ours. Judge City -> Pricing (CF) on that basis.
- Facts you need: all VeloCity teams are in one company, so Customer/Supplier is the default flavour of U/D between internal contexts. The Payment Processor and the City are the only external parties.

**4. OHS / PL**
- Not every upstream must declare them, and they don't have to come together.
- OHS applies only upstream. It is usually combined with PL, because clients need a language to talk to the service, but OHS alone is acceptable.
- PL fits publishing events or schemas. An upstream that publishes events should usually offer PL, and PL alone for event publishing is fine.
- So PL without OHS (Rentals -> Settlement, Settlement -> Accounts) and OHS without PL (Pricing -> Settlement) are not violations in themselves. Do check whether each one matches what actually flows: events or an API.
- An upstream with no pattern at all, like Accounts -> Rentals and CustomerSupport -> FleetMaintenance, is a gap. In both of those, the downstream side is missing too.

**5. Anything else**
- **Unconfirmed relationships:** allowed, but only if they are clearly marked as default assumptions that I haven't confirmed. In CML, that means a comment. The fleet-issue incident route (Customer Support -> Fleet Maintenance -> Rentals) is a documented default, unconfirmed. A name like `FleetIssueIncident_Unconfirmed` is not how I want it marked: the status belongs in a comment, not in the name.
- **Naming:** name each relationship after what flows across it (e.g. `RentalSessionEnded`, `PriceCalculation`), and add a comment listing the messages.
- **Isolated contexts:** a context with no relationship at all is worth flagging.
- **Processes:** the map must support all four processes. Walk each one and map every hand-off:
  1. Onboarding and activation
  2. Rent and return a bike
  3. Settle a ride and recover debt
  4. Bike fault to maintenance
- **Accepted alternatives:** before calling something wrong, check the "Accepted alternatives" section in velocity/velocity-context-map.md. Remember that velocity-vision.md is deliberately inconsistent.
- **What I want back:** your findings and a proposed corrected map as a separate file. Do not change src/main/cml/VeloCity-Context-Map.cml. No canvases.

## Assistant -> expert (message 2)

Thanks, that's what I needed. I checked the map against those rules. A few points need your call before I write up the corrected map.

1. The loop Accounts -> Rentals -> Settlement -> Accounts holds arrow by arrow: Rentals depends on Accounts' model (it asks "is this account active?"), Settlement depends on Rentals' model (it consumes Rental Session Ended), and Accounts depends on Settlement's model (it reacts to Rental Fee Unpaid / Outstanding Debt Cleared). So the "no cycles" claim doesn't hold. My proposed fix: flip the Settlement-Accounts arrow. Accounts offers a generic "block account / lift block (reason: outstanding debt)" open host service in its own language, and Settlement calls it (Accounts upstream, Settlement downstream with an ACL). Settlement still drives both block and unblock for debt. Is that acceptable, or would you rather break the loop elsewhere?

2. CF justifications. My reading of your rules:
   - City -> Pricing: the city's zone map is not a well-established standard and we can't influence it, so Pricing should use an ACL.
   - Rentals -> Settlement, Rentals -> FleetMaintenance, Pricing -> Settlement, Settlement -> DebtRecovery: all internal VeloCity languages, none an established standard, so ACL. (For FleetMaintenance this is clear-cut anyway: it speaks "broken / back in circulation", Rentals speaks "withdrawn / available".)
   - Payments -> Accounts and Payments -> Settlement: Payments publishes a plain payments language (verified / invalidated, charge succeeded / failed) that mirrors the standard payment-provider domain. I'd keep CF there with a written justification. Do you agree, or should those be ACL too?

3. Internal U/D relationships: should I draw them as Customer/Supplier ([S]->[C] in CML) by default? Context Mapper doesn't allow CF inside Customer/Supplier, so the two Payments relationships (if they stay CF) would stay plain U/D.

4. Process 3 ("Settle a ride and recover debt") includes disputes pausing debt recovery, and the reference lists "Billing disputes -> pausing Debt Recovery (from Customer Support)" as deferred, with nothing in the CML. Should I add it as a commented, unconfirmed default (DebtRecovery upstream offering pause/resume via OHS, CustomerSupport downstream with ACL), or leave it out?

5. Process 2: the rider's location is validated against the service area before reserving. Nothing on the map gives Rentals the service area (only Pricing gets the City's zone map). Is that a gap you want on the map, or out of scope?

## Expert reply 2

Domain expert's answers to your 5 questions on the corrected VeloCity map.

1. Loop: agreed, it's real, so the "no cycles" claim in the reference is wrong. Your flip is acceptable, and it's the fix I prefer. Accounts owns blocks and should speak its own generic language: block / lift block, with a reason (invalid card, outstanding debt). This is the same move as Rentals' generic available/withdrawn. Settlement stays the only source of the debt block and its lifting, and calls Accounts' OHS through an ACL. Do the same for the invalid-card block from Payments, so Accounts has one consistent block API. Keep in mind that Payments -> Accounts also covers verifying the payment method at onboarding, so check its direction separately. Don't break the loop at Accounts -> Rentals: "is this account active?" really is Accounts' model.

2. CF justifications: I agree with all of them.
   - City -> Pricing: ACL. The City's zone map is not a standard, and we can't influence it.
   - The internal ones (Rentals -> Settlement, Rentals -> FleetMaintenance, Pricing -> Settlement, Settlement -> DebtRecovery): ACL.
   - Payments -> Accounts and Payments -> Settlement: keep CF, with your written justification that Payments' language mirrors the standard payment-provider domain. That is exactly the "well-established language" case.

3. Yes. Draw internal U/D as Customer/Supplier by default, since all teams are in one company. For the two Payments relationships, plain U/D with CF is fine. Add a CML comment saying they are internal teams and conceptually Customer/Supplier, and that it's not expressible together with CF in Context Mapper.

4. Add it as a commented, unconfirmed default, as you proposed: DebtRecovery upstream, offering pause/resume via OHS; CustomerSupport downstream with an ACL. The vision is explicit that disputes pause recovery, so leaving it off would leave process 3 incomplete. Mark it "default assumption, NOT confirmed with the domain expert" in a comment, not in the relationship name.

5. Not a relationship. The service area is VeloCity's own operating region, our decision, not the City's zone map. Rentals owns it and validates the rider's location against it. Record that in your findings as a decision so nobody draws a City -> Rentals arrow. No new relationship.

Reminder: findings and the corrected map go in a separate file. Don't touch src/main/cml/VeloCity-Context-Map.cml.

## Assistant -> user (final message)

Yes, it breaks some of the rules. The worst problem is a cycle, and the reference doc says there are none. The CML itself is fine: it parses and renders in Context Mapper.

I didn't touch `src/main/cml/VeloCity-Context-Map.cml`. A corrected map is in a separate file, `src/main/cml/VeloCity-Context-Map-Corrected.cml`, and the full findings are in `velocity/velocity-context-map-review.md`.

## Rule violations

1. **Cycle through three contexts: Accounts -> Rentals -> Settlement -> Accounts.** I checked each arrow, and each one is a real dependency on the other context's model:
   - Rentals asks Accounts whether an account is active.
   - Settlement consumes Rentals' *Rental Session Ended*.
   - Accounts conforms to Settlement's *Rental Fee Unpaid* and *Outstanding Debt Cleared*.

   So the "No cycles" claim in `velocity/velocity-context-map.md` is wrong.
   - **Fix:** Accounts offers a generic *block / lift block (reason)* open host service, and Settlement calls it through an ACL. That makes Accounts upstream of Settlement. Settlement is still the only source of the debt block and of lifting it.
   - The invalid-card block keeps going through Payments' *Payment Method Invalidated* event. If Payments called Accounts' block service instead, Payments and Accounts would depend on each other directly.
2. **Two relationships have no patterns on either side:** Accounts -> Rentals and CustomerSupport -> FleetMaintenance. Every downstream must state ACL or CF.
3. **Conformist is used without justification.** ACL is the default, and CF is allowed only towards an established language, with a written reason.
   - **City -> Pricing** should be ACL. The City's zone map isn't a standard, and we can't influence it.
   - **These five should also be ACL,** because they are all internal VeloCity languages:
     - Rentals -> Settlement
     - Rentals -> FleetMaintenance (the clearest case: *withdrawn/available* vs. *broken/back in circulation*)
     - Pricing -> Settlement
     - Settlement -> DebtRecovery
     - Settlement -> Accounts
   - **Payments -> Accounts and Payments -> Settlement** can stay CF. The written reason: Payments' language mirrors the standard payment-provider domain.
4. **An unconfirmed relationship is marked through its name** (`FleetIssueIncident_Unconfirmed`). Mark it in a comment and name the relationship after what flows across it.
5. **Process 3 isn't fully covered.** Disputes pause debt recovery, but nothing in the CML models that. I added it as a commented, unconfirmed default: DebtRecovery is upstream and offers pause/resume through an open host service, and CustomerSupport is downstream with an ACL.

## Smaller points

- **Customer/Supplier:** all VeloCity teams are in one company, so internal U/D should be Customer/Supplier by default. Context Mapper rejects OHS and CF inside Customer/Supplier, so I only drew the two relationships without them as `[S]->[C]`. The others are plain U/D, with a comment saying they are conceptually Customer/Supplier.
- **OHS/PL:** using PL alone (for events) and OHS alone (Pricing, for an API) is fine, and each matches what actually flows.

## Checked and fine

- There is no Shared Kernel, Partnership, Big Ball of Mud or Separate Ways.
- There is no direct A<->B pair, and no context without a relationship.
- The external Payment Processor sits behind an ACL.

## Decision to tell the group

- **The service area isn't a relationship.** It is VeloCity's own operating region. Rentals owns it and validates the rider's location against it, so there should be no City -> Rentals arrow.

## Processes

I walked all four processes through the corrected map (onboarding, rent and return, settle and recover debt, fault to maintenance). Every hand-off has a relationship, and it has no cycles. The bike signalling its own faults to Rentals stays an unconfirmed default, as in the doc.

Before you show it to the group, `velocity/velocity-context-map.md` needs the same changes as the CML: the diagrams, the "No cycles" bullet, and the "Account blocks" decision.
