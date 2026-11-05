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
