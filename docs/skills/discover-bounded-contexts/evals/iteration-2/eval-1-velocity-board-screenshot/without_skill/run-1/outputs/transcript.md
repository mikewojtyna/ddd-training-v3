# Transcript

## User

Here's our VeloCity event storming board: velocity/velocity-processes.jpg. Find the bounded contexts on it. I'm the trainer, by the way, so you can't spoil anything for me.

## Assistant -> user (answered by domain expert)

I've read the board. It has seven swimlanes: User Registration And Account Setup, Bike Rental, Pricing, Bike Station Return Handling, Bike Zone Return Handling, Account Block And Debt Recovery, and Customer Support And Incident Handling.

My first-pass candidates are Accounts, Rentals (reservation, unlock and session, plus both return lanes), Pricing (hourly fees, penalties, drop-zone and unauthorized-zone fees), Debt Recovery, Customer Support, and Fleet Maintenance (maintenance and bike faults). Some parts of the board don't fit that cleanly, though, so before I commit I have a few questions:

1. Nothing on the board actually charges the customer. The flow goes from "Rental Total Fee Calculated" straight to "Account Blocked Due To Debt" and "Account Blocked Due To Invalid Card". What happens in between? Who takes the money, when (per ride, or batched), and through whom (an external payment provider)? Who decides that an unpaid amount has become a "debt"?
2. The Bike Rental lane has "bike broken" leading into "Bike Reserved", and a "maintenance started -> bike fixed -> bike returned" branch after "Bike Unlocked". What really happens there? Who repairs bikes, and is that the same team or process as renting?
3. "Bike Fault Detected / Bike Fault Reported" (station return) and "Customer Support Contacted" (locking failed): who picks these up, support agents or the maintenance crew?
4. Station return and zone return: are these the same "end of rental" with different fee rules, or different processes with different people? Who defines the drop zones and the unauthorized zones?
5. Account blocking: who blocks and unblocks an account? Is blocking for an invalid card handled by the same people as blocking for debt? And what pauses and resumes a debt recovery process?
6. Pricing: who owns the tariff (free period, hourly fees, penalties, zone fees), and how often does it change compared with the rental flow itself?

## Expert reply

1. The charge is made when the rider returns the bike, or when the session is force-ended. The hourly fee stickies during the ride are only an estimate shown to the rider. The money goes through an external payment provider [improvised]. The payment side just moves money and knows nothing about any rules. If a payment stays unsettled, the account gets blocked and debt recovery takes over. Debt recovery has its own rules about which actions to take and on what schedule.

2. A broken bike is really an incident, so it's probably connected to incident handling. Maintenance runs on its own, apart from renting, and is done by a separate maintenance crew [improvised]. The only link to renting is that a rider can report a bike as broken when returning it to a station or a zone.

3. On return, the rental side checks the fault report and publishes an event. Incident handling picks that event up, and the rental side knows nothing more about it. Incidents also come in from riders and from automatic detection. When an incident is resolved, nothing flows back to the other processes. A failed lock goes to customer support the same way, as an incident [improvised].

4. It's the same end of rental with the same rules. The only difference is an extra fee if the bike is left in an unauthorized zone. Our city operations team defines the zones [improvised].

5. An account can be blocked for two reasons: an unsettled payment, or a payment method that wasn't verified in time. Both blocks have the same effect: the account fails the check we run before every rental, so the user can't rent. The system blocks automatically, and the account is unblocked once the debt is paid or the card is verified [improvised]. Debt recovery pauses while the customer is disputing the charge with support, and resumes once the dispute is closed [improvised].

6. Pricing is a simple calculator that any part of the business can use, and it changes independently of the rental flow. The estimate and the final charge use the same rules. The commercial team owns the tariff and changes it far more often than the rental flow changes [improvised].

## Assistant -> user (answered by domain expert)

Thanks, that clears up most of it. I have two follow-ups:

1. Between "rental ended, fee known" and "payment provider moved the money", who owns the charge itself: creating it, deciding when to retry, and declaring a payment "unsettled"? Does the rental side do that, or is there a separate step or team with its own rules (for example, collecting several rides into one charge, or retrying a failed charge)?
2. You said the account is blocked automatically and unblocked once the debt is paid or the card is verified. Does the accounts side hold the "blocked" status and react to news from debt recovery and payment verification, or does debt recovery itself decide who may rent?

## Expert reply

Domain expert's answers to your two follow-ups on settlement and account blocking:

1. The rental side doesn't own the charge. It ends when the fee is known. Settlement is a separate step with its own rules. They aren't very complex, they're mostly about the schedule, and they don't depend on how the rental went. That step decides when to charge, retries a charge that failed, and declares the payment unsettled when it still hasn't gone through [retry/unsettled details improvised]. It currently charges per ride [improvised]. The payment provider only moves the money and knows no rules.

2. The accounts side holds the "blocked" status, and every rental checks that status first, so a blocked user can't rent. Debt recovery doesn't decide who may rent. It runs its own actions on its own schedule. Accounts reacts to news: an unsettled payment or a payment method not verified in time blocks the account, and a cleared debt or a verified card unblocks it [who flips the flag improvised].

## Assistant -> user (final)

With your answers, the board splits into **eight bounded contexts**, plus two external parties. Below is each context, why it is separate, and how it maps back to the swimlanes.

| # | Bounded context | Owns | Board source | Why it stands alone |
|---|---|---|---|---|
| 1 | **Accounts** | Registration, verification (email, phone, payment method), the signed agreement, and the account's *blocked/active* status that is checked before every rental | "User Registration And Account Setup", the "Account Blocked Due To ..." and "Account Block Notification Sent" stickies | It owns the one question "may this user rent?". It blocks or unblocks in response to news from Settlement and from payment-method verification. |
| 2 | **Rentals** | Location validation, reservation, unlock, rental session, and the return to a station **or** a zone (one end-of-rental with the same rules), plus collecting the rider's fault report | "Bike Rental", "Bike Station Return Handling", "Bike Zone Return Handling" | Its pivotal events are *Bike Reserved*, *Rental Session Started* and *Bike Returned*. It ends when the fee is known and does nothing after that. |
| 3 | **Pricing** | The tariff: free period, hourly fees, max-time penalty, drop-zone and unauthorized-zone fees. The same rules give the live estimate and the final fee. | "Pricing" lane, plus "Drop Zone Fee Applied" and "Unauthorized Zone Return Fee Applied" from the zone lane | It is a stateless calculator that the commercial team owns and changes much more often than the rental flow. The hourly stickies are only an estimate. |
| 4 | **Settlement** | Turning a known fee into a charge: when to charge (per ride today), retrying failed charges, and declaring a payment *unsettled* | **Not on the board**. It sits in the gap between "Rental Total Fee Calculated" and "Account Blocked Due To Debt". | It has its own rules, mostly about scheduling, that don't depend on how the ride went. Its pivotal event is *Payment Unsettled*. |
| 5 | **Payments** | Moving money through the external provider, with no business rules | Not on the board (implied by "User Payment Method Verified" and the charge) | It is a generic subdomain: a thin anti-corruption layer (ACL) in front of the **PaymentProcessor**, which is external. |
| 6 | **Debt Recovery** | The recovery process and its actions and schedule: started, paused, resumed, succeeded, failed | "Account Block And Debt Recovery" (from "Debt Recovery Process Started" onward) | It has its own process and policies. It doesn't decide who may rent, because that stays in Accounts. It pauses while a dispute is open in Customer Support. |
| 7 | **Customer Support** | Incidents from riders, from automatic detection and from Rentals (a fault on return, a failed lock): type detection, assignment, escalation, rejection, reopening, resolution. Also charge disputes. | "Customer Support And Incident Handling", plus "Bike Fault Reported" and "Customer Support Contacted" from the return lane, and "bike broken" | Its pivotal event is *Incident Reported*. It reacts to events and sends nothing back when an incident is resolved, except the dispute open/closed signal that Debt Recovery listens to. |
| 8 | **Fleet Maintenance** | Repairing bikes: maintenance started, bike fixed, bike back in service | The misplaced "maintenance started -> bike fixed -> bike returned" branch in "Bike Rental" | It is a separate crew and a separate process from renting. It is not part of a rider's rental. |

**External parties:** the **PaymentProcessor**, reached through Payments, and the **zone map** from city operations. The zone map defines the drop zones and the unauthorized zones that Rentals and Pricing use. I'd model it as upstream reference data. If city operations is really an outside authority, it becomes a second external party.

### What I changed relative to the board

- **The swimlanes aren't the contexts.** The two return lanes merge into Rentals, because it's one end-of-rental with one set of rules. "Account Block And Debt Recovery" splits in two: the block goes to Accounts and the recovery process goes to Debt Recovery.
- **Moved off the Rental lane:** maintenance went to Fleet Maintenance, and "bike broken" went to Customer Support as an incident.
- **Moved off the zone lane:** the two zone fees went to Pricing. Rentals only reports *where* the bike was left.
- **Added what the board is missing:** Settlement and Payments. Without them, nothing explains how "fee calculated" becomes "account blocked due to debt".

### Main flows between the contexts (a first sketch, not a validated context map yet)

- Rentals -> Accounts: "can this user rent?" (Accounts is upstream).
- Rentals -> Pricing: the estimate and the final fee (Pricing is upstream, exposed as an open host service (OHS) with a published language (PL)).
- Rentals -> Settlement: *Rental Ended (fee)* event.
- Settlement -> Payments -> PaymentProcessor: charge requests through an ACL.
- Settlement -> Accounts and Debt Recovery: *Payment Unsettled* event. Accounts blocks, and Debt Recovery starts.
- Debt Recovery -> Accounts: *Debt Cleared* event, and Accounts unblocks.
- Customer Support -> Debt Recovery: *Dispute Opened/Closed* event, which pauses and resumes recovery.
- Rentals -> Customer Support: *Fault Reported* and *Locking Failed* on return.

### Points that rest on your improvised answers

These are worth a second look when you run it with participants:
- Settlement charges per ride today. If it ever batches rides, Settlement grows, but the boundary stays where it is.
- Whether incidents should feed Fleet Maintenance. You said nothing flows back from incident resolution, so I kept Maintenance independent.
- Whether the zone map is an external party or an internal reference-data context.

If you want, I can turn this into a Context Mapper `.cml` model with explicit relationship patterns next.
