I've written the TutorHub context map to `src/main/cml/TutorHub-Context-Map.cml`. Context Mapper loads it with no errors or warnings, and it renders to a PNG.

## Bounded contexts (10 internal, 2 external)

| Context | Team | What it owns |
|---|---|---|
| **TutorVetting** | Trust & Safety | Applications, ID and qualification checks, approval, qualification expiry. It is the **single** place that suspends and reinstates a tutor. |
| **Marketplace** | Marketplace | Tutor profile pages and search. It is a read model built from what other contexts publish. |
| **Scheduling** | Scheduling | Availability calendar, lesson requests and booking, rescheduling (max 2), cancellation rules (free until 24h before, then 50%), reminders. |
| **LessonDelivery** | Scheduling team for now | Video rooms, Lesson Started/Ended, no-show detection (records who didn't show). It sits behind an ACL to the video provider. |
| **Pricing** | Finance | The hourly rate and its history, and the lesson price using the rate valid when the lesson was booked. Commission is 15%, or 10% for tutors rated at least 4.8 with at least 100 held lessons. Quotes, late fees and final charges all use this one calculation. |
| **Billing** | Billing | Pre-authorisation at booking, final charge (2 retries, then unpaid), commission, refunds capped at the amount charged. |
| **Payouts** | Payouts | The earnings balance (it can go negative), Monday payouts (minimum 20 EUR, tax forms), retrying failed payouts the next Monday. |
| **Reviews** | Community | Review requests, reviews within 14 days, rating as the average of the last 50 reviews. |
| **Complaints** | Complaints | Complaints and investigations. It only states outcomes: "upheld against tutor X" and "refund lesson L, amount A". |
| **StudentAccounts** | Platform (generic) | Sign-up, parental confirmation for students under 18, card on file in Stripe. |
| Stripe, VideoProvider | external | Every context that talks to them does so through an ACL. |

## What drove the boundaries
- **The two "Tutor Suspended" stickies** are the same decision made for two reasons. Today two teams make it and overrule each other. TutorVetting now owns it, and Complaints only reports "complaint upheld".
- **Payouts runs on its own clock.** It pays out every Monday whatever happens with lessons, so it is separate from Billing. Billing sends it credits and debits, and Payouts owns the balance.
- **Reviews (up to 14 days later) and Complaints (any time)** each have their own lifecycle that runs well past the end of the lesson.
- **LessonDelivery is split from Scheduling** to isolate the video provider. Its webhooks are unstable (they changed twice this year). It publishes Lesson Held and No-show, which Billing, Reviews and Pricing consume.
- **Pricing changes on Finance's schedule** and is used in three places, so it is an Open Host Service.

## Relationships
- All relationships are upstream to downstream, they form a DAG with no cycles, and there is no Shared Kernel.
- Wherever several consumers rely on a context, it is upstream with OHS/PL: TutorVetting, Pricing, LessonDelivery and Billing.
- To keep the map acyclic, I made three choices and the expert confirmed them:
  1. The rate history lives in Pricing. The tutor still edits the rate, and Marketplace only displays it.
  2. Scheduling books rooms through LessonDelivery's API.
  3. Complaints sends Billing a "refund lesson L, amount A" request and doesn't read data from Billing.

## Open points
- **Overpaid refund requests:** when Complaints asks for more than was charged, Billing refunds what was charged and flags the difference to the Complaints team. I treated that as a notification to the team, not a dependency in the model. If it ever becomes a Billing-to-Complaints event, the map would have a cycle, so it would need another look.
- **Ownership:** LessonDelivery currently belongs to the Scheduling team. That is fine, but keep its model separate.
- **Unconfirmed answers:** the expert marked several answers as improvised and they are worth confirming: what suspension does to lessons already booked, EUR only, refunds after payout pushing the balance negative, student sign-up, and Marketplace ownership.

## Viewing it
Open the `.cml` file in the Context Mapper plugin (VS Code or Eclipse). To get a PNG, copy `VelocityContextMap` and point it at the new file.
