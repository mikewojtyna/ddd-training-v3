# Transcript (baseline, no skill)

## User
Here's our VeloCity event storming board: velocity/velocity-processes.jpg. Find the bounded contexts on it. I'm the trainer, by the way, so you can't spoil anything for me.

_(Assistant read the board image and cropped each swimlane to read the stickies.)_

## Assistant -> user (1)
I've read the whole board (7 swimlanes: User Registration And Account Setup, Bike Rental, Pricing, Bike Station Return Handling, Bike Zone Return Handling, Account Block And Debt Recovery, Customer Support And Incident Handling). Before I propose boundaries I want to check a few things the board alone doesn't settle:

1. Bike Rental lane: "bike broken" sits next to "User Location Validated" as a trigger of "Bike Reserved", and there is a "maintenance started -> bike fixed -> bike returned" branch after "Bike Unlocked". What really happens there? Who runs maintenance, and does a rider ever ride a broken bike to maintenance?
2. Money: the board shows fees being applied (hourly fees, penalty, drop-zone fee, unauthorized-zone fee) and "Rental Total Fee Calculated", but no event where the rider is actually charged, a payment succeeds or fails, or an invoice/receipt is issued. Where does that happen, and who talks to the payment provider? Is the card charged per rental or periodically?
3. "Account Blocked Due To Invalid Card" and "Account Blocked Due To Debt": who decides to block an account, and who unblocks it after the debt is paid? Does debt recovery talk to the rider directly or is it handed to an external agency?
4. Zone returns: who defines the drop zones and the unauthorized zones, and how often do they change?
5. Station return: "Bike Fault Reported" and "Customer Support Contacted" appear there. Do faults go to the same people who handle incidents in Customer Support, or to a separate fleet/maintenance team?
6. Is there anything on the board that you'd say is wrong, missing, or that nobody in the company actually does?

## User (expert) (1)
1. "Bike broken" is there because a rider can't reserve a broken bike. Broken bikes and faults are really incidents. Maintenance runs on its own, separate from rentals. A rider never rides a broken bike to maintenance. The "maintenance started -> bike fixed -> bike returned" branch shouldn't hang off "Bike Unlocked". Our own fleet maintenance crew does that work. [improvised: who runs maintenance, and the point that nobody rides a broken bike there] Also, reserving isn't a phase of its own; it just lets you reserve before you rent. Validating, reserving and unlocking have no extra rules of their own.

2. The real charge happens when the rider returns the bike or the session is forcibly ended. The hourly fees during the ride are only an estimate for the rider's convenience. The estimate and the final charge use the same pricing rules. Pricing is a simple calculator that anyone can use. The payment side only moves money and knows nothing about any of these rules. Settlement has some rules of its own, mostly about the schedule. [improvised: invoices and receipts aren't something we discussed on the board]

3. An account gets blocked for two reasons: a payment wasn't settled, or the payment method wasn't verified in time. The two "Account Blocked" stickies mean the same thing; they just sit in different flows. Debt recovery has its own rules about which actions to take and when. It contacts the rider directly with reminders. After a set time it can hand the debt to an external agency. Once the debt is paid, the account is unblocked automatically. [improvised: direct contact vs. agency, and automatic unblocking]

4. Returns to a station and returns to a zone follow the same rules. The only difference is an extra fee when the bike is left in an unauthorized zone. The operations team draws the zones together with the city. They change rarely, maybe a few times a year. [improvised: who defines the zones and how often they change]

5. A fault goes to incident handling, not into rentals. When a bike is returned, rentals checks whether it's reported broken and publishes an event. Incident handling picks that up, and rentals knows nothing about what happens next. Incidents come from riders reporting them and from automatic detection. Resolving an incident doesn't feed back into any other flow. Maintenance is separate from all of this.

6. Duplicated stickies like "Rental Session Started" mean the same event appearing in different flows; they aren't two different things. The maintenance branch after "Bike Unlocked" is wrong, as I said in answer 1. What's really missing is where the money changes hands after the total is calculated, so that's worth asking about.

## Assistant -> user (2)
Thanks, that clears up a lot. Follow-up on the money, since that's the missing part of the board:

1. After the rental's total fee is known, what happens step by step until the money is collected? You mentioned Settlement "mostly about the schedule": is the rider charged right after each rental, or are rentals collected and charged on a schedule (daily, monthly, once a threshold is hit)? What does settlement do when a charge fails: retry, and when does it give up and start debt recovery?
2. Who actually talks to the card processor: is that an external company? Do "User Payment Method Added / Verified" during registration go through the same payment side?
3. Who decides "Account Blocked": does settlement/debt recovery tell the accounts side to block, or does the accounts side decide on its own? And for "payment method wasn't verified in time" - who notices the deadline passed?
4. Can a blocked rider still rent? Does rentals check the account status before reserving/unlocking?
5. Is fleet maintenance (repairing bikes) something we should model in this system at all, given it's not on the board, or should I leave it out?

## User (expert) (2)
Domain-expert answers to your money follow-up (5 points):

1. The rider's charge is final when the rental ends: either the rider returns the bike or the session is forcibly ended. Settlement then collects that amount. Settlement has its own rules, but they're simple and mostly about the schedule. They don't depend on the rental itself. In practice each finished rental is charged soon after it ends, not in a monthly batch. When a charge fails, settlement retries it a few times over a couple of days. After that it gives up, the payment counts as unsettled and debt recovery takes over. Debt recovery has its own rules for which actions to take and when. [improvised: per-rental charging, and the retry and give-up details]

2. The "payment" side only moves money and knows nothing about any of our rules. The card processing itself is done by an external payment provider. Adding and verifying a payment method at registration goes through the same payment side. [improvised: external provider, and registration sharing the payment side]

3. An account can be blocked for two reasons: a payment wasn't settled, or the payment method wasn't verified in time. The two "Account Blocked" stickies are the same event appearing in two flows. The accounts side does the blocking itself. It reacts to "payment unsettled" news from settlement or debt recovery. It also watches its own verification deadline, so it notices when that has passed. [improvised: who decides and who watches the deadline]

4. A blocked rider can't rent. The account is validated before every rental, and blocked or banned users aren't eligible.

5. Maintenance runs independently of rentals. The only link is that a bike can be reported broken when it's returned to a station or a zone. That report goes to incident handling, not maintenance. It isn't on the board, so leave maintenance out of today's list. [improvised: leaving it out]

## Assistant -> user (3)
Here's my proposed split of the board into bounded contexts. Please tell me where it's wrong.

1. Accounts: registration and setup up to "User Account Activated", the verification deadline, blocking (both reasons) and automatic unblocking when debt is paid, and answering "is this rider eligible to rent?".
2. Rentals: reserve, unlock, ride, end of session (returned to a station or to a zone - one set of return rules, not two), forced end at max rental time. On return it checks the bike's reported condition and publishes a "bike reported broken" event. It asks Accounts for eligibility and Pricing for the fee.
3. Pricing: a stateless calculator - free period, hourly fees, max-time penalty, drop-zone and unauthorized-zone fees. Used both for the in-ride estimate and the final total.
4. Settlement: takes each finished rental's final amount, charges it soon after, retries for a couple of days, then declares the payment unsettled.
5. Payments: only moves money and verifies payment methods; wraps the external payment provider. No business rules.
6. Debt Recovery: starts on an unsettled payment; reminders, pause/resume, handing over to an external agency; success/failure. Its outcome tells Accounts to block/unblock.
7. Customer Support (incident handling): incidents from riders and automatic detection, including bike faults reported on return; type detection, assignment, accept/reject/escalate/reopen/resolve. Doesn't feed back into other flows.

External: the payment provider, the city (co-defines zones with operations), the debt collection agency. Fleet maintenance exists but is left out of this board.

Questions: (a) Do the zone definitions belong in Rentals (it needs to know whether a return spot is authorized) or somewhere else? (b) Does anything look merged that should be split, or split that should be merged?

## User (expert) (3)
Domain expert: your list of 7 contexts is approved. It matches how the business works.

(a) Zone definitions: keep them in Rentals for now. The only thing Rentals needs is whether a return spot is authorized. Pricing then adds the unauthorized-zone fee. Station returns and zone returns follow the same rules apart from that fee. Operations and the city maintain the zone definitions, but rarely, so a separate context isn't worth it today. [improvised]

(b) Nothing needs merging or splitting. Rental preparation (validation, reservation, unlock) has no rules of its own, so keeping it inside Rentals is right. Settlement is its own context with its own schedule rules, apart from Payments, which knows no rules at all. Debt Recovery has its own rules for actions and schedule. Rentals only publishes the fault event and never learns what incident handling does with it. One small wording point: Accounts does the blocking itself, reacting to what Debt Recovery and Settlement report. They don't command it.

## Assistant -> user (4, final)
The board, together with your answers, gives 7 bounded contexts and 3 external parties. Fleet maintenance exists, but we're leaving it off this board.

| # | Bounded context | Owns (stickies / rules) | Talks to |
|---|---|---|---|
| 1 | **Accounts** | Registration and setup up to *User Account Activated*. Watches the payment-method verification deadline. Blocks the account itself, for two reasons: an unsettled payment or a payment method not verified in time. Unblocks automatically once the debt is paid. Answers "is this rider eligible to rent?" | Payments (verify the payment method). Reacts to events from Settlement and Debt Recovery. They don't command it. |
| 2 | **Rentals** | Reserve, unlock, ride, and the end of the session: returned to a station, returned to a zone, or forcibly ended at the max rental time. Station and zone returns are one set of rules. The zone map lives here for now, because Rentals only needs to know "is this return spot authorized?" On return it checks whether the bike is reported broken and publishes a *bike reported broken* event. | Accounts (eligibility). Pricing (estimate and final fee). Publishes *rental ended* for Settlement and *bike reported broken* for Customer Support. |
| 3 | **Pricing** | A stateless calculator with no lifecycle. It covers the free period, the hourly fees, the max-time penalty, the drop-zone fee and the unauthorized-zone fee. The in-ride estimate and the final total use the same rules. | Used by Rentals. Open to anyone who needs a price. |
| 4 | **Settlement** | Collects each finished rental's final amount soon after the rental ends. Retries for a couple of days, then declares the payment *unsettled*. Its rules are about the schedule, not about rentals. | Payments (charge). Publishes *payment unsettled* for Debt Recovery and Accounts. |
| 5 | **Payments** | Only moves money and verifies payment methods. Knows no business rules. Wraps the external provider. | External payment provider. |
| 6 | **Debt Recovery** | *Debt Recovery Process Started / Paused / Resumed / Succeeded / Failed*. Has its own rules about which actions to take and when: reminders to the rider, then handing the debt to an external agency. | Debt collection agency. Its outcomes are what Accounts reacts to. |
| 7 | **Customer Support** (incident handling) | *Incident Reported → Type Detected → Assigned To Agent → Accepted / Rejected / Escalated / Reopened / Resolved*. Incidents come from riders, from automatic detection, and from bike faults reported on return. Nothing flows back into other contexts. | Listens to Rentals' *bike reported broken*. |

**External parties:** the payment provider, the City (it draws the zones together with operations, a few times a year), and the debt collection agency.

### How the board maps onto this (things worth pointing out to participants)

- **Swimlanes aren't contexts one-for-one.** *Bike Station Return Handling* and *Bike Zone Return Handling* both belong to Rentals. The only difference between them is one fee, and that fee lives in Pricing. *Pricing* is a lane in its own right, but its hourly "Fee Applied" events are an estimate, not money.
- **The board has errors on purpose.** *maintenance started → bike fixed → bike returned* hanging off *Bike Unlocked* is wrong: nobody rides a broken bike to maintenance, and maintenance is independent of rentals. *bike broken* as an input to *Bike Reserved* only means "you can't reserve a broken bike".
- **Duplicated stickies mean the same event in two flows.** Examples: *Rental Session Started* in Bike Rental and in Pricing, the two *Account Blocked* stickies, and *Bike Fault Reported* twice.
- **The money is missing from the board.** Nothing on it shows money actually changing hands after *Rental Total Fee Calculated*. Asking about that gap is what brings out Settlement and Payments. Those are the two contexts participants most often miss or merge.
- **Reserve, validate and unlock have no rules of their own**, so they stay inside Rentals and don't become a separate "Reservations" context.

The next step could be a context map with these relationships, written as CML. Or I can hand each context to the scaffolder for a canvas. Tell me which you want.
