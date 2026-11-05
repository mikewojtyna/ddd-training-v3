I put the TutorHub context map in `src/main/cml/TutorHub-Context-Map.cml`. It opens in Context Mapper as it is: the file parses with no errors and renders to PNG.

**Where the boundaries are (11 contexts + 4 external systems)**

| Context | What it owns | How I found the boundary |
|---|---|---|
| TrustAndSafety | Application, documents, qualification checks, approval, qualification expiry, **suspension and reinstatement** | Its own rules (per-country documents, which qualifications count) and its own team. You said you want one place to decide whether a tutor may teach. |
| TutorProfiles | The published profile and the hourly rate, with a history of which rate was valid when | The tutor manages it after approval, and Trust & Safety plays no part. |
| StudentAccounts | Basic sign-up | It has no rules, so off-the-shelf identity is a good fit. |
| Search | Finding tutors by subject, price, rating and free slots | A read model with no rules. It only shows what other contexts publish. |
| Scheduling | Slots, booking, rescheduling (max 2), cancellation (free until 24h before), reminders, running the lesson, no-shows (10 min) | All owned by the Scheduling team. Language: slot, booking, late cancellation, no-show. |
| Pricing | Rate × duration + commission (15%, or 10% for a rating of at least 4.8 and at least 100 lessons held) | Finance changes the commission rules every few months, independently of booking. The same calculation serves the quote, the late-cancellation fee and the final charge. |
| Payments | Saved card, pre-authorization hold, capture, refund at Stripe | Shields TutorHub from Stripe. |
| Billing | Final charge (within 1h, 2 retries, then "unpaid"), commission deduction, tutor earnings, refunds | A separate team with its own rules. |
| Payouts | Monday payout run, 20 EUR minimum, tax forms, retry next Monday | A **pivotal boundary**: it runs on its own clock, "no matter what happens with lessons". |
| Reviews | Review requests, 1-5 ratings with a 14-day window, average of the last 50 reviews | The Community team. Its timeline is decoupled from the lesson. |
| Complaints | Filing at any time, investigation, outcome (rejected / refund / upheld / both) | The Complaints team. It **does not suspend** anyone itself. |
| External | Stripe, StripeConnect, VideoProvider, MessagingProvider | Each one sits behind an ACL. The video provider's unstable webhook model is the main reason for that ACL. |

**Design decisions you should check**
1. **One suspension, two triggers.** The two "Tutor Suspended" stickies were the clue. Complaints now publishes only "Complaint Upheld" (PL), and TrustAndSafety decides who is suspended or reinstated. That stops what happened before, when a renewed certificate re-activated a tutor that Complaints had suspended.
2. **I pulled Payments out of Billing** even though you said Billing uses the card. The hold is placed right after booking, and Scheduling needs the result straight away (a failed hold means the booking doesn't stand). Billing, in turn, depends on Scheduling's lesson outcomes. If Billing owned the hold, Scheduling and Billing would depend on each other in a cycle. With a thin Payments context in front of Stripe, both of them are downstream and the cycle goes away.
3. **The quote at booking is composed in the UI.** Scheduling does not call Pricing. Pricing consumes "Lesson Ended" from Scheduling to count the lessons each tutor has held, so calls in both directions would create another cycle. Billing gets the authoritative price from Pricing, using the rate that was valid at booking time.
4. Every upstream/downstream relationship forms a DAG: there are no cycles and no Shared Kernel. Upstream contexts expose OHS/PL, and downstream contexts are Conformist except toward external systems, where they use an ACL.

**Assumptions to confirm.** Much of the expert's detail was flagged as improvised, so please check these:
- Complaints does not need lesson data from Scheduling, because it only references a lesson id.
- A suspension blocks only *new* bookings. I assumed it doesn't cancel lessons that are already booked or freeze earnings.
- Reminders go out through an external email/push provider that Scheduling calls directly.

A rendered preview is in `outputs/TutorHub-Context-Map.png`. Next, I can hand each context to the bounded-context-scaffolder to write its canvas.
