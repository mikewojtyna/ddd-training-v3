# Transcript

## User

can you sanity-check the VeloCity context map in src/main/cml/VeloCity-Context-Map.cml? I wrote it as the reference solution for the workshop and I want to know if it breaks any of our context mapping rules before I show it to the group.

## Assistant -> Expert (question 1)

Before I check src/main/cml/VeloCity-Context-Map.cml, I need the rules I'm checking against — they aren't written down anywhere in the repo. Could you list your context mapping rules? In particular:
1. Which relationship patterns are allowed or banned (e.g. Shared Kernel, Partnership, Customer/Supplier)?
2. Are upstream/downstream cycles allowed?
3. Where must ACL, CF, OHS and PL sit (upstream vs downstream), and is every upstream required to expose OHS and/or PL?
4. Are unconfirmed / "default assumption" relationships allowed in the reference solution, or must every relationship be confirmed?
5. Anything about external systems (PaymentProcessor, City), context types, or naming that I should check?

## Expert

I'm not going to give you a rulebook of context-mapping patterns. Which patterns are allowed, where ACL, CF, OHS and PL sit, and whether cycles are acceptable are questions for your method, not for me as the business side. Check the map against your own relationship rules, and I'll tell you whether the result matches how the business actually works.

Here is what I can give you:

- **What to check against:** The business facts are in `velocity/velocity-vision.md`. That's my role-play guide, and it is deliberately inconsistent in places. The reference solution is `velocity/velocity-context-map.md`, including its "Accepted alternatives" section. Check the map against these four processes:
  1. Onboarding and activation
  2. Rent and return a bike
  3. Settle a ride and recover debt
  4. Bike fault to maintenance
- **Organisation:** All the VeloCity teams work in one company. The Payment Processor and the City are external parties that we don't control.
- **Confirmed vs. assumed:** If you find something in the map that the documents above don't support, list it as a finding and ask me about that specific case. I'll tell you what the business really does.
- **Deliverable:** Write your findings, plus a proposed corrected map, as a separate file. Don't change `src/main/cml/VeloCity-Context-Map.cml`.
- **Canvases:** There aren't any, so don't expect them.

If you need a business fact while you check, such as what an event means, what order things happen in, which team does what, or which rule applies, ask me and I'll answer.

## Assistant -> Expert (question 2)

Thanks. I've checked the map against the vision and the reference doc, and I have four business questions about things the documents don't settle.

1. Account blocks for debt. When a ride's fee can't be collected after the retries, who decides that the account gets blocked? Does the money side (Settlement) say "block this rider", or does the accounts team own a rule like "a rider with outstanding debt is blocked", with Settlement only reporting the unpaid fee? The same question applies to unblocking when the debt is cleared.
2. Fleet-issue incidents. When support classifies an incident as a fleet issue (for example a broken brake), what happens next, and in what order? Does support hand it to the technicians (Fleet Maintenance), who then take the bike out of circulation? Or does the bike get withdrawn straight away?
3. Billing disputes. The vision says debt recovery can be paused while a dispute is under review. Does a billing-dispute incident in Customer Support actually pause debt recovery today? If so, who triggers the pause and the resume?
4. Faults signalled by the bike itself (a failed lock, or a mechanical fault detected at the station). Who receives that signal first: the rental side (which owns locks and stations) or maintenance?

## Expert

Domain-expert answers to your four questions (blocks, fleet incidents, disputes, bike-signalled faults).

1. Blocking for debt: your second option. The accounts team owns the rule. A rider with outstanding debt is blocked, and a rider with an invalid card is blocked. Those are the only two reasons. The money side doesn't tell anyone to "block this rider". It reports the fact that a ride's fee stayed unpaid after the retries. The accounts team then blocks the account and sends the block notification. Unblocking works the same way. The money side holds the rider's balance, so it is the one that knows when the debt is cleared, even when the payment came through debt recovery. It reports "outstanding debt cleared", and the accounts team lifts the block.

2. Fleet-issue incidents: support hands the case to the technicians. Support never takes a bike out of circulation itself. The order is:
   - support classifies the incident as a fleet issue and hands it over to maintenance;
   - maintenance marks the bike broken and has it withdrawn right away, so it is never offered to riders again;
   - a technician later reserves the bike, unlocks it and repairs it, and only then is it put back into circulation.
   Support keeps the incident and still drives it to resolved.

3. Billing disputes: yes, that's the rule. A disputed debt must not be chased while the dispute is under review. The support agent triggers the pause by accepting the billing-dispute incident. When support resolves the dispute, recovery resumes, or it ends if the dispute cleared the debt. The other reasons to pause (a payment plan being negotiated, a regulatory hold) are handled by the debt recovery people themselves, not by support.

4. Faults signalled by the bike itself: the rental side gets the signal first, because it runs the locks and the stations. It withdraws the bike immediately. For a lock failure during a return, it also ends the session and tells the rider why. Then the technicians are notified. Only maintenance decides when the bike becomes available again.

## Assistant -> User (final)

The map breaks one hard rule: there is an upstream/downstream cycle. It also has a few gaps you should fix before showing it to the group. I haven't changed your file. My findings and a corrected map are in separate files.

**Breaks a rule**
1. **Cycle: Settlement → Accounts → Rentals → Settlement.** It is made of `DebtBlocking`, `AccountStatus` and `RentalSessionEnded`. Your reference doc says "No cycles", but its zoom section only checks the ride-to-money flow, so it misses this one. The expert confirmed that Accounts owns the block rule and only reacts to Settlement's facts, so the Settlement → Accounts edge has to stay. The fix is to invert Accounts ↔ Rentals: Rentals offers reserve and unlock to "any authorised client" through an OHS/PL, and Accounts authorises or revokes the rider (`Rentals [U,OHS,PL]->[D,CF] Accounts`). With that change the map has no cycles.

**Gaps**
2. `Accounts [U]->[D] Rentals` has no patterns on either side. The fix for #1 also fixes this.
3. `City [U]->[D,CF] Pricing`: Pricing conforms to an external party you don't control. I suggest an ACL, the same way Payments protects itself from the Payment Processor.
4. `Pricing [U,OHS]` has no PL. Every other OHS in the map has one.
5. Customer Support → Fleet Maintenance is marked unconfirmed. The expert has now confirmed it: support hands the case over to maintenance and never withdraws a bike itself. Support is the caller, so it is downstream: `FleetMaintenance [U,OHS]->[D,CF] CustomerSupport`.
6. **A relationship is missing.** Accepting a billing-dispute incident pauses debt recovery, and resolving the dispute resumes it or ends it. I added `DebtRecovery [U,OHS]->[D,CF] CustomerSupport : RecoveryPause`.

**Confirmed as correct:** no Shared Kernel or Partnership, the Payment Processor ACL on the downstream side, Settlement → Accounts for blocking and unblocking, and faults signalled by the bike reaching Rentals first.

**Still open:** when a dispute clears a debt, something has to write that debt off in Settlement. The map has no path for this yet, and the expert hasn't been asked who does it.

I haven't run the corrected CML through `VelocityContextMap` yet, so run it before you swap the file in. Both mermaid diagrams and the "Deferred / Open" section in `velocity/velocity-context-map.md` also need the same changes.

Files are in `.claude/skills/discover-bounded-contexts-workspace/iteration-2/eval-3-validate-velocity-reference-cml/without_skill/run-1/outputs/files/`:
- `velocity/velocity-context-map-review.md`: findings, the reasoning behind the cycle fix, and an alternative fix for the debrief
- `src/main/cml/VeloCity-Context-Map.cml`: the proposed corrected map
