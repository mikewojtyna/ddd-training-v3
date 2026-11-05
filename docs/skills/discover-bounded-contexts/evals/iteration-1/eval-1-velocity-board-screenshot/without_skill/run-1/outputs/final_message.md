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
