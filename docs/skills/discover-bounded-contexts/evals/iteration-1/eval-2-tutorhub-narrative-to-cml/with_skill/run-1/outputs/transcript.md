# Transcript: TutorHub discovery (eval 2, with_skill, run 1)

## User (initial)

"I typed up the stickies from our TutorHub event storming in /home/goobar/IdeaProjects/ddd-training-v3/.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md. Can you figure out where the bounded context boundaries are and then draw me a context map I can open in Context Mapper?"

## Facilitator -> user, round 1 (reading + H1)

Thanks for typing these up. Before I apply any heuristic, here's how I read your stickies, so you can correct a misreading cheaply.

**How I read the narrative**
- **Main timeline:** tutor onboarding (Tutor Applied → Identity Documents Uploaded → Qualifications Verified → Tutor Approved → Tutor Profile Published → Hourly Rate Set → Availability Slots Published), then student onboarding (Student Registered → Payment Card Added), then booking (Tutor Searched → Lesson Requested → Lesson Price Quoted → Lesson Booked → Card Pre-authorized → Lesson Reminder Sent), then the lesson (Video Room Opened → Lesson Started → Lesson Ended), then money (Student Charged → Platform Commission Deducted → Tutor Earnings Credited → Weekly Payout Sent), then feedback (Review Requested → Review Submitted → Tutor Rating Updated).
- **Branches:** Lesson Rescheduled / Lesson Cancelled (→ Late Cancellation Fee Applied) after booking; No-show Recorded as an alternative ending to Lesson Ended; Payout Failed as an alternative to Weekly Payout Sent.
- **Clusters that look disconnected:** complaints (Complaint Filed → Complaint Investigated → Refund Issued → Tutor Suspended) and Qualification Expired → Tutor Suspended.
- **Ambiguous:**
  - "Tutor Suspended" appears twice (after a complaint and after a qualification expires).
  - Tutor and student onboarding are written one after the other, but I assume they're independent of each other.
  - Payouts run "every Monday no matter what", reviews arrive "up to 14 days" later, complaints "anytime, weeks later": three different clocks from the lesson.
  - External parties: Stripe (cards, payouts) and a third-party video provider (rooms). I'll treat them as external systems, not contexts we design.

**H1: candidate pivotal events** (a pivot changes the business phase: vocabulary, actors, rules or clock change across it)

| Candidate pivot | Before | After |
|---|---|---|
| Tutor Approved (or Tutor Profile Published?) | vetting a person: documents, qualifications | a tutor who sells lessons: profile, rate, slots |
| Lesson Booked | searching, requesting, quoting | a scheduled appointment: reminders, reschedule, cancel |
| Lesson Ended / No-show Recorded | delivering the lesson (video room) | money: charge, commission, earnings; and feedback |
| Tutor Earnings Credited | per-lesson charging | weekly payouts on Monday's clock |
| Complaint Filed | normal flow | investigation, refund, suspension |

**Questions** (each decides a boundary)
1. Is the real phase change of tutor onboarding **Tutor Approved** or **Tutor Profile Published**? Can an approved tutor exist without a published profile, and who decides each? *(Decides where tutor vetting ends and the tutor's marketplace presence begins.)*
2. What does **Lesson Booked** mean in business terms: is it a firm commitment with its own rules (cancellation windows, late-cancellation fees, reschedule limits)? And does **Card Pre-authorized** have to succeed before a booking counts? *(Decides whether booking/scheduling is a phase separate from search and from the lesson itself.)*
3. Is **Lesson Ended** (or No-show Recorded) the moment the student owes money? Who is "no-show": the student, the tutor, or either, and what happens to the money in each case? *(Decides whether billing is a separate phase after the lesson.)*
4. **Lesson Price Quoted, Late Cancellation Fee Applied, Student Charged, Platform Commission Deducted:** are these computed by one set of pricing/commission rules, or does each have its own? Is the quote a firm price or an estimate? Is the late-cancellation fee actually charged, or only calculated? *(Decides whether there is a separate pricing responsibility.)*
5. **Tutor Earnings Credited → Weekly Payout Sent:** is "earnings credited" a balance the tutor accumulates, which the Monday payout then pays out? What happens on **Payout Failed** (retry, notify, hold)? *(Decides whether payouts are their own phase/lane.)*
6. The two **Tutor Suspended** stickies: is that the same event? Who decides to suspend a tutor, one owner whatever the reason (complaint, expired qualification), or does each department suspend for its own reason? And does suspension cancel future lessons or hide the profile? *(Decides who owns a tutor's standing.)*
7. Does **Tutor Rating Updated** feed back into anything (search ranking, suspension)? *(Decides whether reviews only publish facts, or own a decision.)*
8. Did I misread anything above (order, duplicates, disconnected clusters)?

## Expert -> facilitator, round 1

1. Tutor Approved is the real phase change. Our Trust & Safety team does the vetting (which documents per country, which qualifications count) and approves a tutor once. After that the tutor publishes their own profile and sets their own hourly rate, which they can change any time. So yes, an approved tutor can exist without a published profile for a while. [improvised: the gap between approval and publishing]

2. Yes, Lesson Booked is the real commitment. Before it the student is shopping; after it there's an appointment. The Scheduling team owns the rules: free cancellation up to 24h before, after that a late cancellation fee of 50% of the lesson price, and at most 2 reschedules per lesson. Card Pre-authorized comes right after booking and is just Stripe holding money. The booking doesn't wait for it. [improvised: what happens if the pre-auth fails]

3. Yes. Lesson Ended means the lesson is over and the money side can start. No-show can be either the student or the tutor: whoever didn't join within 10 minutes. We decide that ourselves from the video provider's webhooks. If the tutor no-shows, the student isn't charged. If the student no-shows, they pay in full.

4. It's one calculation: the tutor's hourly rate × duration + platform commission. Commission is 15%, or 10% for tutors with a rating of at least 4.8 and at least 100 lessons. The quote, the late cancellation fee and the final charge all use that same calculation. The rules belong to the business/finance people and change every few months, separately from booking. The quote is a firm price based on the rate valid at booking time. The late fee is really charged. Billing charges it the same way as a lesson.

5. Yes, the credited earnings build up as a balance. Billing credits it per lesson. Every Monday the Payouts team pays it out through Stripe Connect, with a 20 EUR minimum. In some countries tutors need tax forms first. A failed payout is retried the next Monday. That clock has nothing to do with lessons. [improvised: whether we notify the tutor on failure; probably an email]

6. It's the same event with two reasons: an expired qualification, or an upheld complaint. Right now both teams suspend tutors on their own, and it's a mess. Complaints suspended a tutor, then Trust & Safety re-activated them after they renewed a certificate. We want ONE place deciding whether a tutor may teach, and Trust & Safety should own it. Complaints only says "complaint upheld against tutor X". A suspended tutor's profile is hidden from search and they can't receive bookings. [improvised: we'd probably cancel their future lessons too, without a late fee for the student]

7. Yes. Pricing reads the rating for the commission tier (the 4.8 threshold). Search also shows and filters by rating. It doesn't trigger suspension; only Trust & Safety suspends. The Community team owns reviews: 1–5 stars with a comment, up to 14 days after the lesson, and the rating is the average of the last 50 reviews.

8. Mostly right. A few corrections:
   - The complaints cluster isn't really separate. Complaints are filed anytime by students or tutors. The outcome is rejected, refund, upheld against the tutor, or both. The Complaints team decides a refund, but Billing executes it, since Billing knows how to reverse a charge. Suspension after an upheld complaint is decided by Trust & Safety.
   - Search is just finding tutors by subject, price, rating and free slots. It has no rules of its own.
   - Billing does the charging (within 1 hour, two card retries, then mark as "unpaid" and email the student), the commission deduction and the earnings credit. Payouts is a separate team.
   - Tutor and student onboarding are independent of each other. [improvised]
   - Stripe and the video provider are external, as you say.

## Facilitator -> user, round 2 (confirmed pivots, H2, H3 draft)

Thanks, that moved a lot. Here's what changed and where it leads.

**H1: confirmed pivots**
- **Tutor Approved** (confirmed): before it, Trust & Safety vets a person; after it, the tutor runs their own listing (profile, rate, slots). Tutor Profile Published is *not* the pivot, it's the tutor's own step after it.
- **Lesson Booked** (confirmed): shopping before, an appointment with Scheduling's rules after (24h free cancellation, 50% late fee, max 2 reschedules).
- **Lesson Ended / No-show Recorded** (confirmed): the lesson is over and the money side starts.
- **Tutor Earnings Credited** is not a phase change of the lesson; it hands a balance to a different clock (see H2).
- Complaint Filed is not on the main timeline at all; it's a lane (H2).

**H2: swimlanes**
| Flow | Verdict | Why |
|---|---|---|
| Weekly payouts (Payouts team) | lane | own Monday clock, "nothing to do with lessons", own rules (20 EUR minimum, tax forms, retry next Monday) |
| Reviews & rating (Community team) | lane | 14-day window after the lesson, own rule (average of the last 50) |
| Complaints | lane | filed anytime by either side, own outcomes (rejected / refund / upheld) |
| Qualification expiry → suspension | lane, but same owner as vetting | follows the tutor's standing, not any lesson |
| Tutor onboarding vs student onboarding | parallel, independent | different actors and rules |
| Lesson ended vs no-show (student / tutor) | **variants** | same lesson, different endings feeding the same charge rule |
| Lesson vs late cancellation charge | **variants** | "Billing charges it the same way as a lesson" |

**H3: single responsibility, first draft**
Two duplications your answers already resolved:
- **Tutor Suspended** had two owners. You want one: **Trust & Safety** owns "may this tutor teach?", reacting to *Qualification Expired* and to the fact *Complaint Upheld Against Tutor*. Complaints no longer suspends anyone.
- **Price** appeared in the quote, the late fee and the charge. It's one calculation with rules owned by finance that change on their own schedule, so it becomes a separate **Pricing** context used by all three. Pricing decides amounts; it never charges.
- **Search** has no rules of its own, so it isn't a context. It's a read model (subject, price, rating, free slots) over other contexts' facts.

Draft candidates, each with its one question:

| # | Candidate | Question it answers | Owns |
|---|---|---|---|
| 1 | Tutor Trust & Safety | May this tutor teach? | applications, documents, qualification checks, approval, qualification expiry, suspension (any reason), reinstatement |
| 2 | Tutor Listings (profile, rate, slots + search) | What does this tutor offer, and when? | profile published, hourly rate set, availability slots published; search read model |
| 3 | Student Accounts | Who is this student, and can they pay? | student registered, payment card added |
| 4 | Scheduling | Which lesson happens when, between whom? | lesson requested, booked, reminders, rescheduled, cancelled; the 24h / 2-reschedule rules; decides a late cancellation is chargeable |
| 5 | Lesson Delivery | Did the lesson happen, and who showed up? | video room opened, lesson started/ended, no-show (10-minute rule) |
| 6 | Pricing | What does this lesson cost, and what's our cut? | quote, late-fee amount, commission tiers (15% / 10%) |
| 7 | Billing | What does the student owe for this lesson, and is it paid? | student charged (1h, 2 retries, unpaid), commission deducted, earnings credited, refunds executed |
| 8 | Payouts | When and how much do we pay each tutor? | weekly payout, 20 EUR minimum, tax forms, payout failed / retried |
| 9 | Reviews | How good is this tutor in students' eyes? | review requested/submitted, 14-day window, rating (last 50) |
| 10 | Complaints | Was something wrong, and what's the remedy? | complaint filed/investigated, outcome: rejected / refund decided / upheld against tutor |

**Questions** (each decides a boundary)
1. **Availability slots:** are they the tutor's listing (#2), or are they Scheduling's calendar (#4)? Who stops two students from booking the same slot? *(Decides whether slots and double-booking live in Listings or Scheduling.)*
2. **Scheduling vs Lesson Delivery:** is delivery (video room, start/end, the 10-minute no-show rule) owned by the Scheduling team, or by someone else? Do those rules change for different reasons? *(Decides whether #4 and #5 are one context with a pivot inside, like a ride, or two.)*
3. **Tutor listing:** does the tutor's profile/rate have any rules of its own (rate limits, profile review), and which team owns it? Or is it just the tutor's self-service data? *(Decides whether #2 is a context or folds into Trust & Safety / Scheduling.)*
4. **Student accounts:** does student onboarding have rules (verification, age)? When Billing marks a charge "unpaid", can that student still book? If not, who decides that block, Billing, or one owner of "may this student book"? *(Decides whether #3 is a context and who owns student blocking; I don't want a second "Tutor Suspended" situation for students.)*
5. **Card pre-authorization:** who owns it, Billing? And if it fails, does the booking stand, get cancelled, or flagged? *(Decides whether pre-auth is a Billing step or a Scheduling rule.)*
6. **Refunds:** when Complaints decides a refund and Billing reverses the charge, does the tutor's credited earnings balance go down too (and is commission returned)? What if the money was already paid out on Monday? *(Decides the Billing → Payouts hand-off.)*
7. **Pricing's commission tier** needs the tutor's rating (Reviews) and "at least 100 lessons". Who counts lessons: lessons delivered (Delivery) or lessons charged (Billing)? *(Decides Pricing's upstreams.)*
8. **Suspension side effects:** when Trust & Safety suspends a tutor, is it Scheduling that cancels their future lessons (without a late fee), and Listings that hides the profile? *(Confirms they react to one fact instead of deciding the suspension.)*

## Expert -> facilitator, round 2

Round 2 answers from the TutorHub product owner.

1. Tutors publish their free slots themselves; that's their own offer. Once a student books a slot, it's Scheduling's appointment. Scheduling stops two students from booking the same slot, because it's the one confirming bookings. [improvised]

2. No, that's not Scheduling's job. The video side is driven by the third-party provider's webhooks: the room opens 5 min before the start, start and end come from webhooks, and we apply the 10-minute no-show rule ourselves. Those rules change when the provider changes their webhook model, which they've done twice this year. Scheduling's rules (24h, 50%, 2 reschedules) change for business reasons. [improvised: a separate small team looks after the video integration]

3. It's basically the tutor's self-service data. They set their own hourly rate and can change it any time; the rate valid at booking time applies. There's no profile review and no rate limits. Search just shows what tutors publish. [improvised: no rules beyond that; no team really owns it today]

4. Nothing special for onboarding: they register and add a card. [improvised] If a charge ends up "unpaid", the student shouldn't be able to book again until they pay. And yes, that should be one owner, not Billing and Scheduling each blocking on their own. Billing just says "student X has an unpaid lesson". [improvised]

5. Billing. It's just Stripe holding the money right after booking. [improvised] If it fails, the booking is flagged and the student is asked to update their card. If it isn't fixed within 24 hours or before the lesson, Scheduling cancels it free of charge. [improvised]

6. Yes, both go back: the tutor's earnings balance drops by their share and we return our commission. If the money already went out on Monday, the tutor's balance goes negative and the next payout deducts it. Billing does the reversal; Payouts only sees the balance. [improvised]

7. Lessons actually delivered and charged, so no-shows by the tutor and cancellations don't count. I'd take the count from Billing's charged lessons, since that's what the tutor really earned on. [improvised]

8. Yes, that's how it should work. Trust & Safety only says "tutor suspended". The profile disappears from search and they can't take new bookings. Scheduling cancels their future lessons, with no fee for the students. [improvised: the cancellations]

Your draft list looks close to how we work.

## Facilitator -> user, round 3 (H3 corrections, cross-cutting check)

Thanks. Your answers moved four boundaries:

- **Lesson Delivery stays separate from Scheduling.** Its rules (room 5 min before, start/end from webhooks, 10-minute no-show) change when the video provider changes, Scheduling's change for business reasons, and a different team looks after them. Different reasons to change, so two contexts (H3), with *Lesson Booked → Lesson Started* as the hand-off.
- **Tutor Listings has no rules and no owner, so as drafted it isn't a context.** Its pieces already have homes: **availability slots** become Scheduling's (it confirms bookings and prevents double booking, so it must own the calendar the slots live in), and the **hourly rate valid at booking time** is an input Pricing needs. What's left is profile text + search, a read model. See question 1.
- **Student Accounts now has a real decision:** "may this student book?". You want one owner of the unpaid block, like Trust & Safety for tutors. Billing only publishes "student X has an unpaid lesson"; Student Accounts decides the block and lifts it.
- **Card pre-authorization is a Billing step**, and the "not fixed within 24h or before the lesson → cancel free of charge" rule is Scheduling's.

**Cross-cutting check (is every step owned exactly once?)** I walked the narrative and found two things nobody clearly owns yet:
- **The tutor's earnings balance.** Billing credits it and reverses it, and Payouts pays it out and deducts negative balances. That's one piece of state with two writers, the same smell as the double "Tutor Suspended".
- **Billing ↔ Pricing.** Billing needs the price from Pricing, and Pricing needs the charged-lesson count from Billing. If both are live calls, that's a dependency cycle, which usually means a responsibility sits in the wrong place.

**Questions**
1. **Tutor profile + search:** which is closer to the truth? (a) a small **Tutor Catalog** context answering "which tutors can a student find?", owning the profile and the visibility rule (only approved, published, non-suspended tutors with free slots are shown); or (b) the profile is part of the tutor's account in Trust & Safety, and search is just a read model built from published facts. *(Decides whether there is an 11th context or a read model.)*
2. **Earnings balance:** who should own it? My proposal: **Payouts** owns the tutor's balance as a ledger. Billing publishes facts ("earnings credited for lesson L", "earnings reversed for lesson L") and Payouts books them, pays out on Monday and carries negative balances forward. Billing never touches the balance itself. Is that right? *(Decides the single owner of the balance and the Billing → Payouts direction.)*
3. **When is the price fixed?** Is the whole price, commission tier included, frozen in the quote at booking time, so Billing charges what Scheduling booked (and the late fee is 50% of that quoted price)? Or is the commission recalculated at charge time? *(Decides whether Billing needs Pricing at all, which removes the Billing ↔ Pricing cycle.)*
4. **Lesson Requested → Lesson Booked:** does the tutor have to accept a request, or does a booking confirm instantly when the slot is free? *(Small one: it decides whether a "Lesson Request Accepted/Declined" event is missing from the board; it stays in Scheduling either way.)*
5. **Strategic classification (for the map later):** which of these is the core, where TutorHub wins against other tutoring marketplaces: Scheduling, Pricing, Trust & Safety, Reviews, or something else? Which are just necessary? *(Not a boundary question, but it goes into the map and the canvases.)*

## Expert -> facilitator, round 3

Round 3 answers from the TutorHub product owner.

1. (b) is closer. Search is just finding tutors by subject, price, rating and free slots. It has no rules of its own and shows what others publish. The profile is the tutor's own stuff after approval. Hiding suspended tutors follows from Trust & Safety's "suspended" fact; search itself doesn't decide it. [improvised: the profile sitting with the tutor's account]

2. Yes, that's right. Billing does the charging and credits the tutor's share. Payouts is a different team on its own Monday clock, and they should keep the balance, pay it out and carry a negative balance forward. Billing just tells them what happened per lesson. [improvised: Payouts owning the balance]

3. The price is fixed at booking. The rate valid at booking time applies, and the commission tier is fixed in the quote too, so the student pays what they were quoted. [improvised: commission frozen at booking] Billing still uses the same calculation from the pricing rules to work out what's owed: the full price, or 50% for a late cancellation. It doesn't invent its own amounts. For the "at least 100 lessons" count, take delivered lessons from Lesson Delivery if that's cleaner. We only care that tutor no-shows and cancellations don't count. [improvised]

4. It confirms instantly if the slot is free. There's no tutor acceptance step: publishing the slot is the tutor's acceptance. [improvised]

5. Our edge is that students trust our tutors: vetted, with honest ratings. So Trust & Safety, together with Reviews, is what we win on. Pricing with the commission tiers matters to the business, but competitors do similar things. Scheduling, Billing, Payouts and Complaints just have to work. Stripe and video we buy. [improvised]

## Facilitator -> user, round 4 (critic findings)

Before I show you the final list, independent reviewers checked every candidate. Six passed as they are (Trust & Safety, Student Accounts, Lesson Delivery, Payouts, Reviews, Complaints). Three (Scheduling, Pricing, Billing) were held back by the same two open points, and I need you to settle them:

**Questions**
1. **Who owns the 50%?** Right now three places claim the late-cancellation fee: Scheduling ("after 24h, 50% of the lesson price"), Pricing ("same calculation") and Billing ("works out 50% for a late cancellation"). One decision needs one owner. Is the **50%** part of the cancellation policy, changed by the Scheduling team together with the 24h window? Or is it a pricing rule, changed by business/finance together with the commission tiers? *(Either way, Scheduling keeps the "is this cancellation late?" decision and publishes something like "Lesson Cancelled Late", and Billing only charges. This decides whether the percentage lives in Scheduling or in Pricing.)*
2. **Commission on a late fee:** does TutorHub take its commission from a late-cancellation fee, or does the tutor get the whole 50%? *(Decides whether the late-fee amount is a full pricing calculation, i.e. "what's our cut" also covers late fees.)*
3. **After "unpaid":** once Billing has retried the card twice and marked the lesson unpaid, how does the money get collected? Does the student simply update their card and Billing charges again, or is there a collection process with its own rules (reminders over days, more retries, a collection agency, write-off)? *(If it has its own rules and clock, it's a separate context, like debt recovery; if not, Billing owns "Outstanding Payment Settled", which Student Accounts needs to lift the block.)*
4. **Small ones, answer if you know:** (a) Does a suspended tutor still get their balance paid on Monday, or is the payout held? (b) Who decides a refund's amount: Complaints ("refund X EUR") or just "refund lesson L" and Billing reverses it in full? (c) Does a student no-show count as a delivered lesson toward the 100-lesson commission tier? *(Each only adds or removes a relationship on the map.)*

## Expert -> facilitator, round 4

Round 4 answers from the TutorHub product owner.

1. The 50% is part of the cancellation rules, and the Scheduling team owns those, together with the 24h window and the 2-reschedule limit. Pricing only gives the lesson price (rate × duration + commission) that the 50% is applied to. Billing just charges it.

2. Yes, we take our commission from the late fee too, the same way as for a lesson. The tutor gets their share of the 50%, not all of it. [improvised]

3. No collection process. The student gets the "unpaid" email, updates their card, and Billing charges again. Until then they can't book. We don't use agencies, and we don't have write-off rules worth mentioning. [improvised]

4.
   a) Earned money is still paid on Monday. Suspension stops new lessons, not money they've already earned. [improvised]
   b) Complaints decides the amount: "refund lesson L, amount A". It can be partial. Billing just executes it.
   c) Yes. The tutor showed up and the student paid in full, so it counts. Only tutor no-shows and cancellations don't count. [improvised]

## Facilitator -> user, round 5 (candidates for approval, map questions)

Thanks, that settled it: the 50% is Scheduling's cancellation policy, Pricing only supplies the price it applies to (and the commission split, late fees included), Billing just charges, and with no collection process there's no hidden Debt Recovery context. The reviewers re-checked Scheduling, Pricing and Billing and now agree with all nine.

## Proposed bounded contexts (9)

| Context | The one question | Owns | Why it's separate | Critic |
|---|---|---|---|---|
| **Tutor Trust & Safety** | May this tutor teach? | application, identity documents, qualification checks, Tutor Approved, Qualification Expired, **Tutor Suspended (the single owner, any reason)**, reinstatement; hosts the tutor's self-service profile | H1 pivot *Tutor Approved*; H3 resolved the double "Tutor Suspended" (you: "ONE place deciding whether a tutor may teach") | AGREE (r1) |
| **Student Accounts** | May this student book? | Student Registered, Payment Card Added (card reference; card itself at Stripe), **student blocked/unblocked** on Billing's unpaid/settled facts | H3: single owner of the student block (you: "one owner, not Billing and Scheduling") | AGREE (r1) |
| **Scheduling** | Which lesson happens when, between whom? | availability slots, Lesson Booked (instant, no double booking, firm quote), reminders, reschedules (max 2), cancellations: free up to 24h, **late = 50% of the quoted price**; flags/cancels bookings with failed pre-auth; cancels a suspended tutor's lessons free | H1 pivot *Lesson Booked*; H3: Scheduling team's rules change for business reasons | AGREE (r2, after the 50% fix) |
| **Lesson Delivery** | Did the lesson happen, and who showed up? | Video Room Opened (5 min before), Lesson Started/Ended from provider webhooks, **No-show Recorded (10-minute rule, student or tutor)** | H1 pivot *Lesson Ended*; H3: changes when the video provider changes, separate team | AGREE (r1) |
| **Pricing** | What does a lesson cost, and what's our cut? | Hourly Rate Set (rate valid at booking time), Lesson Price Quoted (firm, tier frozen), commission tiers 15% / 10% (rating ≥ 4.8 and ≥ 100 delivered lessons, student no-shows count), commission split of any charged or refunded amount | H3: one calculation reused by quote, late fee and charge; finance rules change independently | AGREE (r2) |
| **Billing** | What does the student owe for this lesson, and is it paid? | card pre-authorization, Student Charged (within 1 h, 2 retries → Lesson Marked Unpaid), Outstanding Payment Settled, commission deducted, Lesson Earnings Recorded/Reversed, refunds executed | H1: far side of *Lesson Ended*; has its own timing/retry rules | AGREE (r2, after the 50% and "unpaid" fixes) |
| **Payouts** | When and how much do we pay each tutor? | **tutor earnings balance (ledger, sole owner)**, Weekly Payout Sent every Monday (20 EUR minimum, tax forms), Payout Failed → retried next Monday | H2 lane: own Monday clock, own team | AGREE (r1) |
| **Reviews** | How good is this tutor in students' eyes? | Review Requested/Submitted (1–5 stars, 14-day window), Tutor Rating Updated (average of last 50) | H2 lane: 14-day clock, Community team | AGREE (r1) |
| **Complaints** | Was something wrong, and what's the remedy? | Complaint Filed (anytime, either side), Investigated, outcome: rejected / **Refund Decided (amount, may be partial)** / **Complaint Upheld Against Tutor**; never suspends, never moves money | H2 lane: anytime clock, own team; H3: publishes outcomes only | AGREE (r1) |

**Not contexts:** tutor search (a read model over published facts, no rules), notifications/reminders/emails (a mechanism). **External:** Stripe (cards, pre-auth, charges, Connect payouts) and the video provider.
**Classification (your words):** core = Trust & Safety + Reviews; Pricing supporting ("competitors do similar"); Scheduling, Lesson Delivery, Billing, Payouts, Complaints, Student Accounts supporting ("just have to work"); Stripe and video bought.

**Open assumptions (non-blocking):** reschedule keeps the original quote and pre-auth; tax forms are collected by Payouts; reviews only after a delivered lesson.

**Suggested board changes:** merge the two "Tutor Suspended" stickies (one event with a reason); rename "Late Cancellation Fee Applied" → *Lesson Cancelled Late*; rename "Tutor Earnings Credited" → *Lesson Earnings Recorded*; split "No-show Recorded" into *Student No-show* / *Tutor No-show*; fold "Lesson Requested" into *Lesson Booked*; add *Card Pre-authorization Failed*, *Lesson Marked Unpaid*, *Outstanding Payment Settled*, *Student Blocked/Unblocked*, *Refund Decided*, *Complaint Upheld Against Tutor*, *Complaint Rejected*, *Lesson Earnings Reversed*, *Tutor Application Rejected*, *Qualification Renewed*, *Tutor Reinstated*.

## Questions
1. **Do you approve these nine contexts?** If anything is off, tell me which one and why.
2. You asked for a context map: I'll write it in **Context Mapper (.cml)** format at `src/main/cml/TutorHub-Context-Map.cml`. To test it I'll walk these **business processes** through it. Are these the right ones, and is anything missing?
   - P1 Onboard a tutor (apply → approved → profile, rate, slots)
   - P2 Book and take a lesson (search → quote → book → pre-auth → reminder → lesson → charge → earnings)
   - P3 Cancel or reschedule a lesson (free vs late cancellation, late fee charged)
   - P4 Pay tutors weekly (balance → Monday payout → failure/retry)
   - P5 Review a tutor (review → rating → commission tier, search)
   - P6 Handle a complaint (outcome → refund and/or suspension → lessons cancelled)
   - P7 Qualification expires (suspension → profile hidden, lessons cancelled)
   - P8 Recover an unpaid lesson (unpaid → student blocked → card updated → settled → unblocked)
3. **Team relationships:** are all nine contexts built by internal TutorHub teams who plan together (then I'll use Customer/Supplier with an anti-corruption layer on the downstream side as the default)? Or is any of them run by an external party or a team you can't influence?
4. **External APIs:** for Stripe I'd let Billing, Payouts and Student Accounts *conform* to Stripe's model (it's a well-established payment language), and put an **anti-corruption layer** between Lesson Delivery and the video provider (whose webhook model changed twice this year). OK?

## Expert -> facilitator, round 5

Round 5 answers from the TutorHub product owner.

1. Yes, I approve the nine. That's how we work. I'm not fussed about the exact names.

2. Yes, please make the context map in CML. The system name is TutorHub. The four processes that matter most to me are:
   - Tutor onboarding (your P1)
   - Book and take a lesson (P2)
   - Weekly payout (P4)
   - Complaint to refund and suspension (P6)

   P3, P5, P7 and P8 are fine as extra checks, but start with those four.

3. All nine are our own teams in the same company, on the same product, and we plan together. One thing: nobody wants a shared database or a shared code library between teams. The last shared "common" module was a disaster.

4. OK. Stripe's API is an industry standard and we're happy to follow it in the payment code. The video provider's webhook model is messy and they've changed it twice this year, so yes, keep it at arm's length.

And I'd like the canvases for each context at the end.

## Facilitator -> user, round 6 (map questions from the critics)

I've drafted the map (`src/main/cml/TutorHub-Context-Map.cml`, valid in Context Mapper, 9 contexts + Stripe + the video provider, 18 relationships, all Customer/Supplier with a published language upstream and an anti-corruption layer downstream, Stripe as the only Conformist, no shared kernel). Reviewers walked your four processes through it: tutor onboarding and complaints passed; booking and weekly payout raised questions only you can answer.

**Questions**
1. **Does Scheduling check the quote when a student books?** For example: is the quote for this tutor and this lesson length, and is it still valid? Or does the booking just keep the quote's number, and Billing (which pre-authorizes the quoted price) is the first to look at the amount? *(If Scheduling checks it, Scheduling depends on Pricing, and the map has a loop: Pricing → Scheduling → Lesson Delivery → Pricing (the delivered-lesson count). That's acceptable if explained, but I don't want to hide it behind a guess.)*
2. **Tax forms and payout accounts:** who collects and checks a tutor's tax forms, and who sets up their Stripe Connect payout account: Payouts, or Trust & Safety during vetting (with the per-country documents)? How does Payouts know a tutor's country? *(Decides whether Trust & Safety → Payouts is a relationship.)*
3. **Who adapts to whom when Complaints hands over an outcome?** My draft has Complaints use Trust & Safety's own intake ("report: complaint upheld against tutor X") and Billing's refund request ("refund lesson L, amount A"), so Complaints adapts to their language, rather than those teams having to understand Complaints' model. Is that how the teams would work? *(Decides the direction of two arrows; the other direction would create a loop Trust & Safety → Scheduling → Complaints → Trust & Safety.)*
4. **Rate before bookable:** can a tutor's slots be booked before they've set an hourly rate, or does Scheduling have to refuse that? *(If Scheduling refuses, it needs Pricing's rate facts, same loop as in Q1.)*

## Expert -> facilitator, round 6

Round 6 answers from the TutorHub product owner.

1. Yes. Scheduling needs the price from Pricing. The student sees the quote and books at that price, so Scheduling has to get a quote for this tutor and this lesson length from Pricing. It doesn't make up its own number. [improvised: the validity check] On the lesson count: I don't mind where Pricing gets it, as long as tutor no-shows and cancellations don't count. If counting charged lessons from Billing avoids your loop, that's fine too. [improvised]

2. Payouts collects the tax forms and sets up the Stripe Connect payout account. That's their job, not Trust & Safety's. [improvised] They get the tutor's country from Trust & Safety, which already knows it from vetting (the documents are per country). [improvised]

3. Yes, that's how it would work. Trust & Safety decides suspensions and Billing moves money, so Complaints sends them "complaint upheld against tutor X" and "refund lesson L, amount A" in their terms. [improvised: the direction]

4. No. A slot without a rate can't be booked, because there's no price to quote. Scheduling refuses it. [improvised] It needs the price from Pricing anyway, as in question 1.

## Facilitator -> user, round 7 (map for approval)

Thanks. Your answers changed the map: Scheduling gets the quote from Pricing again, Trust & Safety now tells Payouts the tutor's country, and the Complaints arrows follow the direction you confirmed. A final review walked all eight processes through it and agreed.

## TutorHub context map (`src/main/cml/TutorHub-Context-Map.cml`, valid in Context Mapper)

All internal relationships are **Customer/Supplier** (your teams plan together): the upstream publishes a documented language, and the downstream keeps an anti-corruption layer. There's **no shared kernel**, as you asked: no shared database or shared library.

| Upstream → Downstream | What flows | Why this direction |
|---|---|---|
| Stripe → Billing, Payouts, Student Accounts (**Conformist**) | cards, pre-auth, charges, refunds, Connect payouts | Stripe is an industry standard and you're happy to follow it |
| Video provider → Lesson Delivery (**ACL**) | room / join / leave webhooks | messy webhook model, changed twice this year: kept at arm's length |
| Trust & Safety → Scheduling | Tutor Approved / Suspended / Reinstated | Scheduling refuses suspended tutors and cancels their lessons |
| Trust & Safety → Complaints | "complaint upheld against tutor X" intake | Complaints speaks Trust & Safety's terms (your round 6 answer) |
| Trust & Safety → Payouts | tutor's country | Payouts decides who needs tax forms |
| Student Accounts → Scheduling | Student Blocked / Unblocked | blocked students can't book |
| Student Accounts → Billing | card reference, card updated; Billing reports unpaid / settled in Student Accounts' terms | Student Accounts alone decides the block |
| Pricing → Scheduling | Lesson Price Quoted (no rate → no quote → not bookable) | Scheduling books at Pricing's price |
| Pricing → Billing | quoted price, commission split of any charged or refunded amount | Billing doesn't invent amounts |
| Reviews → Pricing | Tutor Rating Updated | rating ≥ 4.8 tier |
| Scheduling → Lesson Delivery | booked / rescheduled / cancelled lessons | room 5 min before |
| Scheduling → Billing | Lesson Booked (pre-auth), Lesson Cancelled Late (50%), free cancellations (release); Billing reports a failed/confirmed payment guarantee in Scheduling's terms | Scheduling owns the 50% and the "cancel if not fixed in 24h" rule |
| Scheduling → Complaints | lesson details | who taught whom, when |
| Lesson Delivery → Billing | Lesson Ended, Student / Tutor No-show | full charge vs no charge |
| Lesson Delivery → Reviews | Lesson Ended | review request, 14-day window |
| Lesson Delivery → Pricing | lesson outcomes | Pricing counts towards the 100-lesson tier |
| Billing → Payouts | Lesson Earnings Recorded / Reversed | Payouts owns the balance |
| Billing → Complaints | "refund lesson L, amount A" | Complaints speaks Billing's terms |

**Validator findings, and how they're resolved**
- 3× *conformist-check* (Stripe): justified by your answer, Stripe's API is an industry standard.
- 2× *indirect cycle* Pricing → Scheduling → Lesson Delivery (→ Reviews) → Pricing: **kept on purpose, explained in the CML.** It's your business's own feedback loop: lesson history sets the commission tier, which is frozen into the next quote. Every arrow is a real dependency on a different thing (quote, appointment, lesson outcomes). Counting lessons from Billing instead would make it worse (a direct loop, because Billing already depends on Pricing for the split).
- Context Mapper also warns "ACL in a Customer/Supplier relationship": we keep the ACLs on purpose, so a supplier's change doesn't leak into its customer's model.

**Open assumptions (marked in the CML)**
1. A reschedule keeps the original quote and pre-authorization.
2. When a pre-authorization fails, Billing tells Scheduling in Scheduling's terms ("payment guarantee failed for booking B"), the same pattern you confirmed for Complaints, so Scheduling stays upstream of Billing.

**Questions**
1. Do you approve this map?
2. Are the two open assumptions right? If not, tell me which.
3. Next, I'll create a Bounded Context Canvas for each of the nine contexts, pre-filled from this session (canvases only, no code yet). OK?

## Expert -> facilitator, round 7

Round 7 answers from the TutorHub product owner.

1. Yes, I approve the map. It matches how our teams work, and it keeps the video provider at arm's length without any shared database or shared library.

2. Both assumptions are fine.
   - A reschedule keeps the original quote and pre-authorization. The student shouldn't pay more just for moving the lesson. [improvised]
   - Billing telling Scheduling "payment guarantee failed for booking B" is how I'd expect it to work. [improvised]

3. Yes, please do the canvases for all nine. No code yet.

## Facilitator -> user, final message

Done. Your TutorHub board splits into **9 bounded contexts**, and the context map opens in Context Mapper.

**Files**
- Context map: `src/main/cml/TutorHub-Context-Map.cml`. Valid (0 errors): 9 contexts plus Stripe and the video provider, 20 relationships.
- Discovery report: `docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md`. It's the full trail (heuristics, your answers, reviewer verdicts, process walkthroughs, validator output) and the place to resume from.
- Canvases (drafts for your review; no plan or code yet): `docs/bounded-contexts/tutorhub/{tutortrustandsafety, studentaccounts, scheduling, lessondelivery, pricing, billing, payouts, reviews, complaints}/canvas.md`

**The contexts and the question each answers**
- **Tutor Trust & Safety:** may this tutor teach? It's the only place a tutor gets suspended.
- **Student Accounts:** may this student book?
- **Scheduling:** which lesson happens when, between whom? It owns the 24h rule, the 50% late fee and the 2-reschedule limit.
- **Lesson Delivery:** did the lesson happen, and who showed up?
- **Pricing:** what does a lesson cost, and what's our cut?
- **Billing:** what does the student owe for this lesson, and is it paid?
- **Payouts:** when and how much do we pay each tutor? It owns the tutor's balance.
- **Reviews:** how good is this tutor in students' eyes?
- **Complaints:** was something wrong, and what's the remedy?

Search and notifications aren't contexts. Stripe and the video provider are external.

**The map**
- Every internal relationship is Customer/Supplier: the upstream publishes a documented language and the downstream keeps an anti-corruption layer.
- Stripe is the only Conformist.
- The video provider sits behind an anti-corruption layer.
- There's no shared kernel.
- There are no two-way dependencies between any pair of contexts.
- One loop is kept on purpose, with the reasoning written in the CML: Pricing → Scheduling → Lesson Delivery → Pricing. It's your own feedback loop: lesson history sets the commission tier, and the tier is frozen into the next quote.

**What the reviews changed**
- **The 50% late fee:** Scheduling, Pricing and Billing all claimed it. Your answer gave it to Scheduling.
- **Unpaid lessons:** a reviewer asked whether they needed a debt-recovery context. They don't, because there's no collection process.
- **Quote reference:** my first fix for the loop was a guessed "quote reference" that hid it. A reviewer rejected that, and your answer brought back Pricing → Scheduling with the loop explained.
- **Tax forms:** these added a Trust & Safety → Payouts relationship for the tutor's country.

**Still open** (listed in the report and the canvases)
1. Is the commission added on top of rate × duration, or taken out of it? Your answers said "rate × duration + commission" and also "commission deducted". This affects the numbers in the Pricing and Billing rules.
2. Review moderation, and whether a student can review a tutor who didn't show up.
3. Complaints against students, and complaint investigation rules.
4. Tutor-side cancellations and reschedules.
5. A cap on refunds.
6. Which countries need tax forms.

Several rules are still prose rather than Given/When/Then and are marked that way in the canvases.

**Suggested board changes:** merge the two "Tutor Suspended" stickies, rename "Late Cancellation Fee Applied" to *Lesson Cancelled Late*, split "No-show" into student and tutor, and add the missing events (Lesson Marked Unpaid, Refund Decided, Complaint Upheld Against Tutor, …). The full list is in the report.

Next step: review the canvases. When one is ready, ask me to scaffold that context, and bounded-context-scaffolder will write its plan and stop for your review before any code.

## Critic verdicts (one line each: subject | round | verdict | gist)

- Reviews | round 1 | AGREE | one question, own 14-day lane, core; notes: who may review after tutor no-show, review moderation, rename to Tutor Rating Recalculated
- Payouts | round 1 | AGREE | sole owner of balance ledger, own Monday lane; notes: who owns tax forms / Connect account, held payouts for suspended tutors, missing events (Earnings Reversed, Payout Skipped/Held)
- Tutor Trust & Safety | round 1 | AGREE | single owner of "may this tutor teach?"; straddling Tutor Approved acceptable (profile side has no rules); notes: missing Application Rejected, Qualification Renewed, Tutor Reinstated; say profile hosting is convenience
- Student Accounts | round 1 | AGREE | single owner of "may this student book?", small supporting; notes: add Student Blocked/Unblocked, Outstanding Charge Settled, Payment Card Updated; card reference owned here, not in Billing
- Complaints | round 1 | AGREE | own lane and team, publishes outcome only; questions: who sets refund amount, complaints against students; add outcome events
- Lesson Delivery | round 1 | AGREE | own rules/team/reason to change, wraps video provider via ACL; notes: publish facts not the tier count, split No-show into Student/Tutor No-show
- Pricing | round 1 | DISAGREE | 50% late-cancellation rule owned by Pricing, Scheduling and Billing; questions: who changes the 50%, commission on late fees
- Scheduling | round 1 | DISAGREE | same 50% late-fee ownership duplication with Pricing; Scheduling should own only the lateness decision; notes: add Pre-authorization Failed/Succeeded, cancellation reasons
- Billing | round 1 | DISAGREE | 50% late fee has three owners; unpaid lesson has empty far side (how it gets collected); rename Tutor Earnings Credited, pre-auth failure as Billing fact, Scheduling flags booking
- Pricing | round 2 | AGREE | 50% now owned once (Scheduling); counting rule and commission split in Pricing; note: late-fee split uses tier frozen in the original quote
- Scheduling | round 2 | AGREE | lateness + 50% owned once; notes: rename Late Cancellation Fee Applied to Lesson Cancelled Late, explicit Booking Flagged, fold Lesson Requested into booking
- Billing | round 2 | AGREE | 50% gone, unpaid far side closed (no collection process); notes: partial refund split via Pricing, add Card Pre-authorized after fix, watch Billing<->Scheduling direction on the map
- Map + P4 Weekly payout | round 1 | DISAGREE | tax-form rule needs tutor country/tax-form status with no relationship, assumption unmarked; ask who collects tax forms / Connect account
- Map + P1 Tutor onboarding | round 1 | AGREE | every step owned, TutorStanding carries hand-off; question: must a rate exist before slots are bookable (would reopen Pricing->Scheduling)
- Map + P6 Complaint | round 1 | AGREE | every hand-off carried, no cycle; note: mark ComplaintReporting/Refunds directions as unconfirmed facilitator choices
- Map + P2 Book and take a lesson | round 1 | DISAGREE | opaque quote reference (no Pricing->Scheduling) is an unconfirmed assumption that hides a cycle; ask whether Scheduling must check the quote; phrase pre-auth feedback in Scheduling's API language
- Map v3 | (facilitator) | n/a | after expert round 6: Pricing->Scheduling restored, cycle kept and justified, TutorCountry added
- Map v3 + all processes P1-P8 | final pass (round 2) | AGREE | every step owned, no unused relationship, kept indirect cycle adequately justified; notes: fix 'core' wording, rename DeliveredLessonCount, mark reschedule-keeps-quote and pre-auth feedback direction as unconfirmed
