# Transcript

## User
can you sanity-check the VeloCity context map in src/main/cml/VeloCity-Context-Map.cml? I wrote it as the reference solution for the workshop and I want to know if it breaks any of our context mapping rules before I show it to the group.

## Facilitator -> expert (message 1)
Happy to check src/main/cml/VeloCity-Context-Map.cml before the workshop. I've read it alongside velocity/velocity-context-map.md, but your context mapping rules aren't written down anywhere I can see, so before I judge anything: what are the rules the map has to follow? In particular:
1. Which relationship patterns are allowed or banned (e.g. Shared Kernel, Partnership, Customer/Supplier, Separate Ways)?
2. Are cycles in upstream/downstream relationships forbidden — and do you mean any directed cycle across several contexts (A->B->C->A), or only two contexts pointing at each other?
3. Where may ACL / Conformist / OHS / PL sit — ACL and CF only on the downstream side, OHS/PL only on the upstream side? Must every relationship name its patterns on both ends, or is a bare [U]->[D] fine?
4. Anything else (e.g. how to treat unconfirmed relationships like the CustomerSupport -> FleetMaintenance one, or rules about external systems)?

## Expert reply 1
I'm the business side, so I can't set modelling rules for you. Questions about which relationship patterns are allowed, what counts as a cycle, or where ACL, Conformist, OHS and PL may sit are for the method you're running the check with. You'll have to apply your own rules. I'll answer business questions and tell you how things actually work here.

Here are the business facts that matter for this review:
- **Teams:** All VeloCity teams are in one company. The Payment Processor and the City are external.
- **Processes:** Check the map against these four:
  1. Onboarding and activation
  2. Rent and return a bike
  3. Settle a ride and recover debt
  4. Bike fault to maintenance
- **Sources:** The business facts are in velocity/velocity-vision.md and velocity/velocity-context-map.md. The vision document is deliberately inconsistent. The context map includes its "Accepted alternatives" section.
- **Output:** Put your findings and a proposed corrected map in a separate file. Do not change src/main/cml/VeloCity-Context-Map.cml.
- **Canvases:** Don't produce any for this review.

About the support relationship you flagged as unconfirmed: if you want to know what support staff actually do when a rider reports a problem with a bike, ask me and I'll walk you through it.

## Facilitator -> expert (message 2)
Thanks, understood: I'll apply the modelling rules myself. A few business questions so I check the map against how things actually work:
1. Fleet-issue incidents: when a rider reports a problem with a bike to support, what do support staff actually do, step by step? Who marks the bike broken and who tells the technicians?
2. Billing disputes: when a rider disputes a charge through support, does that pause debt recovery? Who decides and who triggers the pause?
3. Renting: when a rider tries to unlock a bike, who checks that the account is active and not blocked, and does the rental side ever need to tell the accounts side anything back?
4. Debt cleared: when a rider pays their outstanding debt (during recovery or otherwise), who is the source of the fact "debt is paid" that lifts the block?

## Expert reply 2
Domain expert answers to your four business questions (fleet issues, disputes, unlocking, debt paid).

1. Fleet-issue incidents. A rider reports something like a broken brake. The incident's type is detected as a fleet issue and it is assigned to a support agent. The agent accepts it, or rejects it, in which case the rider can push back and the incident may be reopened. Once it's accepted, the agent doesn't fix anything. They escalate it to the specialised team, which here means the technicians' side. From that moment the bike counts as broken and riders can no longer rent it. A technician then reserves it, unlocks it and starts maintenance. Once the bike is fixed, the technician puts it back into circulation. The incident is resolved from there, and every incident must end up resolved. Support staff decide the report is a real fleet issue and hand it over. Support agents never repair bikes, and they don't decide when a bike comes back.

2. Billing disputes. The rider complains about a wrongful charge. The incident is typed as a billing dispute and a support agent accepts it. While the dispute is under review, the active debt pursuit is paused: no reminders, no pushing for payment. The account stays blocked during the pause. A pause isn't a write-off. Once the dispute is settled, recovery resumes and ends in success or exhaustion. Support raises the fact that a dispute is under review, and the debt-recovery people put the pursuit on hold because of it. A regulatory hold or a payment plan being negotiated pauses recovery the same way, and those come from the recovery people themselves.

3. Renting. When a rider unlocks, we must make sure the account is fully activated (every onboarding step done) and not blocked. That information belongs to whoever looks after accounts, onboarding and blocks, and the rental side only asks. Nothing flows back. Renting never activates, blocks or unblocks an account. An account is blocked for only two reasons: an invalid card, or outstanding debt from a ride that couldn't be charged.

4. Debt cleared. All money is collected through the same place that charges rides and keeps the rider's balance and outstanding debt. The debt-recovery people chase the rider, send reminders and negotiate plans, but when money is actually taken they ask that billing side to collect it. They don't move money themselves. So the fact that the debt is paid comes from the billing side that owns the balance, and that fact lifts the debt block. This is true whether the rider paid during recovery or at any other time.
