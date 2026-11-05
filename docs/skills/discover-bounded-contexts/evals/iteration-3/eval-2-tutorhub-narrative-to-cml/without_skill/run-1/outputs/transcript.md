# Transcript (baseline, no skill)

User: "I typed up the stickies from our TutorHub event storming in /home/goobar/IdeaProjects/ddd-training-v3/.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md. Can you figure out where the bounded context boundaries are and then draw me a context map I can open in Context Mapper?"

---

## Facilitator -> Expert (message 1)

Thanks for the typed-up stickies. Before I draw boundaries I have a few questions where the board alone doesn't tell me enough:

1. "Tutor Suspended" appears twice: once after a complaint is investigated and once after a qualification expires. Is it the same suspension (same people deciding, same consequences, same way to lift it), or two different things? Who decides each one, and what does a suspended tutor lose (profile hidden, existing lessons cancelled, payouts frozen)?
2. "Lesson Price Quoted": how is the price worked out? Is it simply the tutor's hourly rate times duration, or are there discounts, platform fees, currency rules, etc.? Who owns those rules?
3. Money flow: who is the merchant of record when a student is charged? After "Student Charged" the commission is deducted and tutor earnings credited. Is the tutor's earnings balance a ledger you keep yourselves, with Monday payouts drawing from it? What happens when a payout fails, and when a refund is issued after the tutor has already been paid?
4. Reviews and ratings: who uses the tutor rating (search ranking, suspension decisions, anything else)?
5. Complaints: who handles them (a support team?), and what outcomes can an investigation have besides refunds and suspensions?
6. Lesson lifecycle: who decides a no-show, and who decides the late-cancellation fee (the rule and the amount)? Does the video provider tell you when a lesson actually started and ended?
7. Student side: is "Student Registered" / "Payment Card Added" a simple account sign-up, or is there more to it (verification, parents/guardians for minors, etc.)?
8. Are there any teams/departments in the company that own parts of this (e.g. tutor onboarding/compliance team, finance, trust & safety, support)?

## Expert -> Facilitator (reply 1)

1. It's two different reasons. One is that a qualification expired: under the Trust & Safety rule, the tutor stays suspended until they renew it. The other is a complaint being upheld. Right now Trust & Safety and Complaints each suspend tutors on their own, and it's a mess. We had a tutor suspended by Complaints who got re-activated by Trust & Safety after renewing a certificate. We want one place deciding whether a tutor may teach, owned by Trust & Safety. Complaints should only say "complaint upheld against tutor X". A suspended tutor's profile is hidden from search and they can't get new bookings. [improvised] Lessons already booked are cancelled with no charge to the student, and payouts of earnings already made aren't frozen.

2. It's the tutor's hourly rate times the duration, plus our commission. The rate is the one valid at booking time, since tutors can change their rate whenever they like. Commission is 15%, or 10% for tutors with a rating of at least 4.8 and at least 100 lessons. The business/finance people set the commission rules and change them every few months, separately from booking. We use the same calculation for the quote, the late cancellation fee and the final charge. [improvised] Everything is in EUR, and there are no student discounts.

3. [improvised] We're the merchant of record; the charge goes through our Stripe account. After the lesson, Billing builds the final charge, charges the pre-authorised card, takes off our commission and credits the tutor's earnings. They have to charge within an hour and retry the card twice; after that it's marked "unpaid" and we email the student. Yes, we keep the earnings balance ourselves. Every Monday the Payouts team pays out what has built up, through Stripe Connect. The minimum is 20 EUR, and tutors in some countries need tax forms first. A failed payout is retried the next Monday. [improvised] If a refund comes after the tutor has been paid, Billing reverses the charge and the tutor's balance goes negative, so it's taken out of their next payouts.

4. Search uses the rating for filtering and ranking. Pricing uses it for the commission tier. It's the average of the last 50 reviews. Students can rate a lesson 1-5 with a comment for up to 14 days afterwards. The small Community team owns reviews. [improvised] The rating isn't used for suspensions; only upheld complaints and expired qualifications lead to one.

5. Students or tutors can file a complaint at any time, and the Complaints team investigates. The outcome is one of four: rejected, refund, complaint upheld against the tutor, or both refund and upheld. When there's a refund, Complaints only says "refund lesson L, amount A" and Billing actually reverses the charge. An upheld complaint leads to a suspension, but Trust & Safety decides that.

6. We decide no-shows ourselves from the video provider's webhooks: anyone who hasn't joined within 10 minutes is a no-show. If the tutor doesn't show, the student isn't charged. If the student doesn't show, they pay in full. The Scheduling team owns the cancellation rules: cancelling is free up to 24h before, after that the fee is 50% of the lesson price, and there's a maximum of 2 reschedules per lesson. The fee amount comes from the same price calculation. And yes, the provider opens the room 5 minutes before the start and sends us Lesson Started / Ended. Their webhook model is messy, though, and they've changed it twice this year.

7. [improvised] It's a simple sign-up: email, name and a card. The card is stored and pre-authorised by Stripe, not by us. Students under 18 need a parent's email to confirm the account, but that's all there is to it.

8. Yes, they're all teams in the same company working on the same product:
- Trust & Safety: tutor applications, ID and qualification checks, approval and suspensions.
- Scheduling: booking, cancellation and rescheduling rules.
- Finance: pricing and commission rules.
- Billing: charges and refunds.
- Payouts: Monday payouts.
- Community: reviews.
- Complaints: complaint investigations.

Stripe and the video provider are external.

---

## Facilitator -> Expert (message 2)

Thanks, that clears up a lot. A few follow-ups on stickies that none of the teams you listed obviously owns:

1. Tutor Profile Published, Hourly Rate Set, Availability Slots Published, Tutor Searched: who owns the tutor's public profile, rate, availability calendar and search? Is that Trust & Safety, Scheduling, or some other team (e.g. a marketplace/catalog team)?
2. Video Room Opened, Lesson Started, Lesson Ended, No-show Recorded: is that the Scheduling team's work, or does someone else own the "lesson actually happening" part and the integration with the video provider?
3. The tutor earnings balance: Billing credits it and Payouts pays it out. Which of the two actually owns the balance (the ledger)? Does Payouts just ask "how much does tutor X have?" or does it keep its own record?
4. Card Pre-authorized at booking: is that Billing's job, triggered when Scheduling books the lesson?
5. Student Registered / Payment Card Added, and Lesson Reminder Sent: which team owns student accounts, and who sends reminders?
6. For the commission tier, Pricing needs the tutor's rating and lesson count. Where does the "at least 100 lessons" number come from: lessons that were held (delivered), or lessons that were paid for?

## Expert -> Facilitator (reply 2)

TutorHub expert, answers to your 6 follow-ups.

1. [improvised] Once Trust & Safety approves a tutor, the tutor runs their own profile and hourly rate. A small Marketplace team builds the profile pages and search. Search only shows what others publish: the profile and rate from the tutor, the rating from Community, and the free slots. The tutor manages their availability calendar, and Scheduling owns it, because a slot disappears as soon as it's booked.

2. [improvised] At the moment the Scheduling team runs the video provider integration and the no-show check. The provider opens the room 5 minutes before the start and sends us webhooks for Started and Ended. We count a no-show ourselves when someone hasn't joined within 10 minutes. Their webhook model is messy and they've changed it twice this year, so whoever owns the integration feels that pain.

3. [improvised] Payouts owns the earnings balance. Billing tells them "credit tutor X with amount Y" after a charge, or "debit tutor X with amount Y" after a refund. Payouts keeps the balance and its own record of what it has paid out. On Monday they pay what's in it, minimum 20 EUR. If the balance is negative, nothing goes out until it's back above zero.

4. [improvised] Yes. Billing places the pre-authorisation on the card when the lesson is booked. It's only Stripe holding the money, and Billing captures it later when it builds the final charge.

5. [improvised] We don't have a dedicated team for student accounts. It's a basic sign-up run by the web/platform team, and Stripe stores the card. Scheduling sends lesson reminders to the student and the tutor, 24h and 1h before the lesson.

6. [improvised] It's lessons actually held, meaning Lesson Ended and the tutor showed up. Paid status doesn't matter, so a student who never paid still counts towards the tutor's 100.

---

## Facilitator -> Expert (message 3)

Here's my draft split, plus three design choices I'd like you to confirm or push back on.

Draft bounded contexts:
- Tutor Vetting (Trust & Safety): applications, ID and qualification checks, approval, qualification expiry, and the single decision on suspension/reinstatement ("may this tutor teach?").
- Marketplace (Marketplace team): tutor profiles and search, a read model built from what others publish.
- Scheduling (Scheduling team): availability calendar, lesson requests, booking, rescheduling, cancellation rules and late-fee decisions, reminders.
- Lesson Delivery (run by the Scheduling team for now, but a separate model): video rooms, Started/Ended, no-show detection, shielded from the video provider by an anti-corruption layer.
- Pricing (Finance): lesson price, commission tiers, used for quotes, late fees and final charges.
- Billing: pre-authorisation, final charge, commission, refunds, unpaid charges.
- Payouts: tutor earnings balance, Monday payouts, retries, tax-form gating.
- Reviews (Community): review requests, reviews, tutor rating.
- Complaints: complaints, investigations, outcomes.
- Student Accounts (platform team, generic): sign-up, parental confirmation, card on file via Stripe.
- External: Stripe and the video provider.

Design choices to confirm:
A) The tutor's hourly rate, with its history ("the rate valid at booking time"), lives in Pricing. The tutor sets it there, and Marketplace only displays it. Otherwise Scheduling, Marketplace and Pricing end up depending on each other in a circle. Is Finance OK owning the rate history, even though the tutor edits the rate?
B) Lesson Delivery offers a "book a room for lesson L at time T with these participants" service that Scheduling calls, and it publishes Lesson Held / No-show. Pricing (held-lesson count), Reviews (review request) and Billing (charge or no charge) consume those. OK?
C) Complaints don't pull lesson or charge data from other teams. The complaint carries the lesson reference, and the investigator sets the refund amount. Billing checks it against the actual charge when it reverses. Does that match how the Complaints team works?

## Expert -> Facilitator (reply 3)

TutorHub expert: the list looks right to me and matches how our teams work. You can go ahead with it, and the names don't matter much to me.

A) That's fine. Finance doesn't mind keeping the rate and its history, as long as the tutor can still change their rate whenever they like and the rate valid when the lesson was booked is the one used. That rate feeds the quote, the late fee and the final charge. [improvised]

B) Yes, that fits. One thing matters a lot to Billing: a no-show has to say who didn't turn up. If it was the tutor, the student isn't charged. If it was the student, they pay in full. Only lessons that were actually held should count towards the tutor's 100 lessons and lead to a review request.

C) Yes, that's how the Complaints team works today. [improvised] The complaint carries the lesson reference, the investigator chooses the amount, and Complaints just says "refund lesson L, amount A". Billing won't refund more than was actually charged for that lesson, so if Complaints asks for more, Billing refunds the charged amount and flags the difference to the Complaints team.

---

## Assistant -> User (final message)

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
