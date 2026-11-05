# Transcript: TutorHub discovery session (2026-11-05)

## User (initial request)

"I typed up the stickies from our TutorHub event storming in /home/goobar/IdeaProjects/ddd-training-v3/.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md. Can you figure out where the bounded context boundaries are and then draw me a context map I can open in Context Mapper?"

## Facilitator -> user, message 1 (reading of the input + H1)

Thanks, I've read the TutorHub stickies. Before I apply any heuristic, here's how I read the board, so you can correct me cheaply now.

**How I read it**
- **Main timeline (one lesson):** Tutor Searched -> Lesson Requested -> Lesson Price Quoted -> Lesson Booked -> Card Pre-authorized -> Lesson Reminder Sent -> Video Room Opened -> Lesson Started -> Lesson Ended -> Student Charged -> Platform Commission Deducted -> Tutor Earnings Credited -> Review Requested -> Review Submitted -> Tutor Rating Updated.
- **Branches off the main timeline:** Lesson Rescheduled (loops back before the lesson), Lesson Cancelled -> Late Cancellation Fee Applied, and No-show Recorded as an alternative ending to Lesson Started/Ended.
- **Prerequisite flows before any lesson:** tutor onboarding (Tutor Applied -> Identity Documents Uploaded -> Qualifications Verified -> Tutor Approved -> Tutor Profile Published -> Hourly Rate Set -> Availability Slots Published) and student onboarding (Student Registered -> Payment Card Added). I read these two as independent of each other.
- **Disconnected clusters:** weekly payouts (Weekly Payout Sent / Payout Failed, "every Monday no matter what"), complaints (Complaint Filed -> Complaint Investigated -> Refund Issued / Tutor Suspended, "anytime, even weeks later"), and qualification expiry (Qualification Expired -> Tutor Suspended).
- **External parties:** Stripe (cards and payouts) and the video provider (rooms). I'll treat them as external systems, not contexts we design.
- **Ambiguous:** "Tutor Suspended" appears twice; I can't tell whether Late Cancellation Fee Applied and Student Charged are real charges or just records; Payout Failed has nothing after it; I can't tell whose no-show "No-show Recorded" means.

**Heuristic 1: pivotal events (candidates only, you confirm)**
A pivotal event changes the business phase: the vocabulary, the actors, the rules or the clock differ before and after it. My candidates:
- **Tutor Approved** (or Tutor Profile Published): before it, we vet a person; after it, they sell lessons.
- **Lesson Booked**: before it, a student shops for a tutor; after it, there's a commitment with a time slot and money held.
- **Lesson Ended / No-show Recorded**: before it, the lesson is being delivered; after it, it's about money and feedback.
- **Student Charged / Tutor Earnings Credited**: before it, the lesson's money is owed; after it, it's the tutor's balance waiting for Monday's payout.

**Questions** (each one decides a boundary)
1. Is the real phase change on the tutor side **Tutor Approved** or **Tutor Profile Published**? Who verifies qualifications, and are there rules (document types, what counts as qualified, expiry dates)? *Decides whether tutor vetting is its own context or part of the tutor's marketplace profile.*
2. Can a student search and book before **Payment Card Added**, and does student registration have any rules of its own? *Decides whether student onboarding is a phase worth a boundary or just an account.*
3. What does **Lesson Requested** mean: does the tutor have to accept it, or does a request become a booking automatically? Is **Lesson Booked** the moment of commitment? *Decides where the booking pivot really sits.*
4. Who decides the **Lesson Price Quoted**: is it just hourly rate x duration, or are there other rules (discounts, packages, platform fees)? Does the same calculation decide the late-cancellation fee? *Decides whether pricing is a separate responsibility.*
5. **Card Pre-authorized** and **Student Charged**: when exactly is the student charged (after Lesson Ended, after a no-show, at cancellation)? Is **Late Cancellation Fee Applied** a real charge or only recorded? *Decides who owns charging the student.*
6. **No-show Recorded**: whose no-show (student, tutor, both)? Who decides it, and on what evidence (for example the video room)? What follows for each kind? *Decides whether lesson delivery ends at Lesson Ended / No-show Recorded and who owns the outcome.*
7. **Platform Commission Deducted -> Tutor Earnings Credited -> Weekly Payout Sent**: is there a tutor balance that collects earnings over the week and is paid out on Monday? Who sets the commission rate? What happens after **Payout Failed** (retries, holding the money, contacting the tutor)? *Decides whether payouts are their own context with their own rules and clock.*
8. **Tutor Suspended** appears twice. Is it the same event? Who decides a suspension, and what does it do (hide the profile, cancel booked lessons, hold payouts)? Can a tutor be reinstated? *Decides who single-handedly owns "can this tutor teach?".*
9. **Complaints**: who investigates, and what can the outcome be? Who decides a **Refund Issued**, and does it take money back from the tutor's earnings? *Decides whether complaints are their own lane and how they feed back into money and tutor status.*
10. **Reviews**: does **Tutor Rating Updated** affect anything else, such as search ranking or suspension? *Decides whether reviews are a separate lane or part of the tutor profile.*
11. **Lesson Reminder Sent**: does it have any rules beyond "send a reminder before the lesson"? *Decides whether it's just a mechanism.*

## Expert -> facilitator, reply 1

Mostly right. Here are the answers.

1. Approval is the big moment, and it happens once. Our Trust & Safety team handles applying, ID upload, qualification checks and approval. They have their own rules: which documents each country needs and which qualifications count. After approval the tutor publishes a profile and sets their own hourly rate. They can change the rate any time.

2. [improvised] They can browse and search without a card, but they need a card to book because we pre-authorise it right away. Registration itself is just an account, with no special rules I know of.

3. A request isn't a commitment. The student asks for a slot, sees the quote and books, and the tutor doesn't have to accept separately. Lesson Booked is the real commitment: before it the student is shopping, after it there's an appointment.

4. The price is the tutor's hourly rate x duration + platform commission. Commission is 15%, or 10% for tutors with a rating of 4.8 or more and at least 100 lessons. The business/finance people set the commission rules and change them every few months, separately from booking. The same calculation gives the quote, the late cancellation fee and the final charge.

5. Card Pre-authorized happens right after booking and is just Stripe holding the money. The real charge comes after Lesson Ended, a no-show or a late cancellation. The Billing team builds it (lesson price or fee), charges the card within 1 hour, deducts commission and credits the tutor's earnings. If the card fails they retry twice, then mark the lesson "unpaid" and email the student. So yes, the late cancellation fee is a real charge: 50% of the lesson price if the student cancels less than 24h before.

6. It can be either side. A no-show means the student or the tutor didn't join within 10 minutes, and we decide that ourselves from the video provider's webhooks. If the tutor doesn't show, the student isn't charged. If the student doesn't show, they pay in full.

7. Yes, earnings pile up and the Payouts team pays them out every Monday through Stripe Connect. Payouts start at 20 EUR, and tutors in some countries need tax forms first. A failed payout is retried the next Monday. Finance sets the commission rate. Payouts run on their own clock, completely separate from lessons.

8. There are two reasons: a qualification expired (suspended until renewed), or a complaint was upheld. Today both teams suspend tutors on their own and it's a mess. Once a tutor suspended by Complaints was re-activated by Trust & Safety after renewing a certificate. We want ONE place deciding whether a tutor may teach, owned by Trust & Safety. Complaints should only say "complaint upheld against tutor X". A suspended tutor is hidden from search and can't get bookings. Renewing the qualification lifts that kind of suspension. [improvised] For complaint suspensions, Trust & Safety decides on reinstatement.

9. Students or tutors can file anytime, and the Complaints team investigates. The outcome is rejected, refund, complaint upheld against the tutor, or both refund and upheld. Complaints decides a refund ("refund lesson L, amount A"), but Billing does the actual reversal because Billing knows how to reverse a charge.

10. Yes, it feeds pricing: the rating decides the commission tier. Search also shows and filters by rating. It doesn't suspend anyone. The small Community team owns reviews. Students rate 1-5 with a comment, up to 14 days after the lesson, and the rating is the average of the last 50 reviews.

11. [improvised] No, it's just a reminder before the lesson, with no rules we care about.

On your other points: Stripe and the video provider are external, as you say. Search has no business rules; it just shows tutors by subject, price, rating and free slots. Cancellation is free up to 24h before the lesson, and a lesson can be rescheduled at most twice. The Scheduling team owns those rules.

## Facilitator -> user, message 2 (H1 confirmed, H2 swimlanes)

Thanks, that moved a lot. Here's what changed and why, then the second heuristic and the questions it raises.

**What your answers changed (H1, pivotal events)**
- **Tutor Approved is confirmed as a pivot.** Before it, Trust & Safety vets a person with country-specific document and qualification rules. After it, the tutor sells lessons and sets their own rate. Tutor Profile Published is just the first step after it.
- **Lesson Booked is confirmed as a pivot.** Before it the student is shopping (search, request, quote); after it there's an appointment with Scheduling's rules (free cancellation up to 24h, at most two reschedules).
- **Lesson Ended / No-show Recorded / late cancellation are the hand-off to money.** All three end the appointment and start Billing's work: build the charge, charge within 1 hour, retry twice, mark "unpaid".
- **Tutor Earnings Credited hands off to Payouts,** which runs on its own Monday clock.
- **Tutor Suspended:** one event, one owner (Trust & Safety), two reasons. Complaints only publishes "complaint upheld against tutor X". That's an H3 result we'll keep.
- **Pricing looks like its own responsibility:** one calculation used for the quote, the late-cancellation fee and the final charge, with commission rules that Finance changes every few months, separately from booking.
- **Not contexts:** search has no rules (it's a view over tutors, prices, ratings and free slots), the reminder is a mechanism, Stripe and the video provider are external.

**Heuristic 2: swimlanes (flows on their own clock)**
- **Payouts:** every Monday, regardless of lessons. A lane.
- **Reviews:** up to 14 days after the lesson, owned by Community. A lane.
- **Complaints:** anytime, even weeks later, with its own investigation. A lane.
- **Qualification expiry:** runs on the certificate's own clock, but it's Trust & Safety's rule about whether a tutor may teach, so I read it as part of the vetting context, not a lane of its own.
- **Student vs tutor no-show, on-time vs late cancellation:** I read these as **variants** of how an appointment ends (same flow, different charge), not separate lanes.

**Questions** (each one decides a boundary)
1. **Tutor profile, hourly rate and availability slots:** who owns them after approval? Trust & Safety, Scheduling, or someone else? Are there rules there (what a profile must contain, rate limits, how slots are published)? *Decides whether there's a separate "tutor offer/catalog" context, or whether the profile belongs to Trust & Safety and the slots to Scheduling.*
2. **Lesson delivery:** who owns the video room and the 10-minute no-show rule? The Scheduling team, or a different team? Does that part change for its own reasons (for example a new video provider or new attendance rules)? *Decides whether Lesson Delivery is separate from Scheduling, or Lesson Booked -> Lesson Ended is one context.*
3. **Tutor earnings balance:** Billing credits earnings and Payouts pays them out. Who owns the balance itself, that is, who answers "how much do we owe tutor X right now"? *Decides whether the balance lives in Billing or in Payouts. It can't be owned by both.*
4. **Refund after a payout:** when Complaints decides a refund and Billing reverses the charge, what happens to the tutor's earnings if they were already paid out (negative balance, deducted from the next payout, absorbed by the platform)? *Decides how Billing and Payouts depend on each other.*
5. **"Unpaid" lessons:** does an unpaid lesson stop the student from booking again, or is it only an email? If it blocks, who decides that block? *Decides whether there's a student-eligibility responsibility with an owner, or student accounts are just a generic account.*
6. **Lessons count for the commission tier** ("at least 100 lessons"): which lessons count (completed only, or also no-shows and late cancellations), and who is the source of that count? *Decides which context Pricing depends on for the tier.*
7. **Suspension and existing bookings:** when a tutor is suspended, what happens to their already-booked lessons? Are they cancelled, and by whom? *Decides whether Scheduling reacts to Trust & Safety's suspension, and whether there's a missing event.*
8. **Missing events:** may I suggest adding these stickies to the board? Tutor Reinstated, Complaint Upheld, Refund Decided (by Complaints) vs Refund Issued (by Billing), Payment Failed / Lesson Marked Unpaid, Lesson Price Calculated, Payout Retried. *Each one marks a hand-off between candidates.*

## Expert -> facilitator, reply 2

Here are my answers to your second round of questions.

1. [improvised] Once approved, tutors manage their own profile, hourly rate and free slots, and Trust & Safety doesn't touch them again. The rate can change any time, and the rate valid at booking time applies. Slots are the tutor's free times; once a student books one, Scheduling's booking rules apply.

2. [improvised] Today the Scheduling team looks after it as well. The provider opens the room 5 minutes before the start, and Lesson Started / Ended come from their webhooks. We decide the 10-minute no-show ourselves from those webhooks. Their webhook model is messy and they've changed it twice this year, so that part keeps getting rewritten for their reasons, not ours.

3. [improvised] Payouts answers "how much do we owe tutor X". Billing tells them what each lesson earned, and Payouts keeps the running total it pays out every Monday.

4. [improvised] If the money was already paid out, the refunded amount is taken off the tutor's next payout. If it's more than that payout, the tutor goes negative and future earnings pay it back. The platform doesn't absorb it.

5. [improvised] Today an unpaid lesson only triggers the email. It doesn't stop the student from booking again.

6. [improvised] Only lessons that actually took place (Lesson Ended) count. No-shows and late cancellations don't. The count comes from the record of lessons that ended.

7. [improvised] All their future booked lessons are cancelled automatically, the students aren't charged anything, and they're told to rebook with someone else.

8. Yes, please add them, they're all real moments for us. One note: a failed payout is only retried on the next Monday, not straight away.

## Critic verdicts, contexts round 1

- Reviews | r1 | AGREE | own question, own 14-day lane and Community team; non-blocking Qs: which endings open a review, can reviews be removed
- Complaints | r1 | AGREE | own team, own clock, decides outcome and refund but neither suspends nor moves money; non-blocking Qs: refund amount rule, tutor-filed complaints
- Payouts | r1 | AGREE | owns the balance, own Monday lane and rules; note: rename Billing's "Tutor Earnings Credited" to "Lesson Earnings Determined" so the balance has one owner; Qs: payouts held on suspension? who collects tax forms?
- Tutor Vetting | r1 | AGREE | single owner of "may this tutor teach"; straddling Tutor Approved justified by same team/rules; suggest "Qualification Renewed" event; Qs: automatic suspension on Complaint Upheld? two reasons at once?
- Pricing | r1 | AGREE | one calculation for quote/late fee/final charge, Finance changes it independently; Qs: is the commission tier frozen at booking? who owns the 50% fee amount
- Scheduling | r1 | AGREE | straddles Lesson Booked but shopping side has no rules (H3); keep slots here; suggest Lesson Cancelled Late / Lesson Cancelled By Platform events; Qs: pre-auth failure, tutor cancellations
- Billing | r1 | AGREE | owns charge/retry/unpaid/reversal rules, Stripe external; reword question to separate from Pricing; Qs: pre-auth failure, pre-auth release, commission on refunds
- Lesson Delivery | r1 | DISAGREE | provider webhook churn is an ACL concern, not an H3 signal; same team; Lesson Started not a confirmed pivot; blocking Qs to expert: who owns attendance rules, is Lesson Started a phase change, technical-failure rules
- Tutor Profiles | r1 | DISAGREE | expert grouped free slots with tutor self-service, so availability may belong here, not Scheduling (conflicts with Scheduling critic's note); blocking Qs: slot management vs double booking, profile rules/owner

## Facilitator -> user, message 3 (H3 draft, critic findings, open doubts)

Thanks. With your answers I drafted nine candidate contexts (H3: one question each) and had each one reviewed by an independent critic. Seven passed. Two need your input before I can show you the list, because the answers decide where a boundary sits.

**Where things stand (H3, single responsibility)**
- **Payouts owns the tutor balance** (your answer 3). So Billing's sticky "Tutor Earnings Credited" really means "this lesson earned X"; Payouts credits the balance. I'll suggest renaming it to **Lesson Earnings Determined** so the balance has one owner.
- **Pricing** owns one calculation (quote, late fee, final charge, commission tier); **Billing** only decides *which* charge applies to how the lesson ended, collects it, retries, marks unpaid, and reverses refunds.
- **Students:** an unpaid lesson only triggers an email and registration has no rules, so I'm treating student accounts as a generic account, not a context we design.

**Doubt 1: is "lesson delivery" (video room, started/ended, 10-minute no-show) its own context, or part of Scheduling?** The critic pointed out that the only reason to split it so far is the video provider's webhook churn. That's a technical reason, and it's handled by a translation layer towards the provider, not by a separate context. The same team runs it and nobody confirmed Lesson Started as a phase change.

**Doubt 2: who owns the tutor's free slots?** You said tutors manage "profile, hourly rate and free slots" themselves, and Scheduling's rules apply once a slot is booked. That could put availability with the tutor profile, or with Scheduling, which has to guarantee a booked slot isn't free any more.

**Questions** (1-5 decide boundaries; 6-10 decide relationships on a map later)
1. **Attendance rules:** who decides the attendance rules (the 10-minute threshold, what counts as a lesson that took place, disputes over a no-show), and do they change separately from the cancellation/reschedule rules? *Business rules with their own owner or reason to change would make Lesson Delivery its own context; otherwise it folds into Scheduling.*
2. **Is Lesson Started a phase change?** Once a lesson has started, do Scheduling's rules stop applying and do different rules or people take over? *Decides whether there's a pivot inside the appointment.*
3. **Technical failures:** when the room doesn't open or the provider is down, is there a rule (free reschedule, refund, partial charge)? Who decides? *Same: own rules would support a separate delivery context.*
4. **Free slots:** can a tutor add, change or withdraw free slots at any time? What happens if they withdraw a slot while a student is booking it? Is "never double-book" a hard rule? *Decides whether slots belong to the tutor profile or must be decided together with bookings in Scheduling.*
5. **Tutor profile:** does the profile have any rules (e.g. only subjects the tutor is qualified for, minimum/maximum rate, currency)? Which team owns it after approval? *Decides whether Tutor Profiles is a (small) context of its own or only data behind the search view.*
6. **Pricing at booking:** is the whole price (rate *and* commission tier) frozen at booking, or is the commission recalculated at charge time? *Decides whether Billing takes the amount from the booking or asks Pricing again.*
7. **Complaint Upheld:** is the tutor suspended automatically, or does Trust & Safety review each case? If a tutor is suspended for both reasons, is the suspension lifted only when both are cleared? *Shapes the Complaints -> Trust & Safety hand-off.*
8. **Payouts and suspension / tax forms:** are payouts held while a tutor is suspended? Who collects the tax forms, Payouts or Trust & Safety? *Decides whether Payouts depends on Trust & Safety.*
9. **Pre-authorisation:** if it fails right after booking, is the booking void, and who cancels it? Is the hold released when a lesson is cancelled for free? *Decides whether Scheduling depends on Billing at booking time.*
10. **Refunds:** when Billing reverses a charge, is the platform commission reversed too, or does the tutor carry the whole refunded amount? *Decides what Billing tells Payouts.*

## Expert -> facilitator, reply 3

Here are my answers to your H3 questions (doubts 1 and 2, plus questions 6-10).

1. [improvised] The Scheduling team set the 10-minute rule when they set up the cancellation rules, and nobody has touched it since. It doesn't change on its own. A lesson counts as having taken place when both people joined. If someone disputes a no-show, they file a complaint and the Complaints team handles it.

2. [improvised] No. Once a lesson starts, it's still the same appointment with the same people, and nothing new kicks in until Lesson Ended.

3. [improvised] If the room doesn't open or the provider is down, the student gets a free reschedule and isn't charged. That reschedule doesn't count toward the 2-reschedule limit. Scheduling decides that.

4. [improvised] Tutors can add, change or withdraw free slots any time, but they can't withdraw a slot that's already booked. Never double-booking is a hard rule. If two people go for the same slot, whoever books first gets it and the other is told it's gone.

5. [improvised] The only rule is that a tutor can list only subjects Trust & Safety verified. There's no minimum or maximum rate, and everything is in EUR. After approval no team really owns the profile: tutors edit it themselves.

6. [improvised] The whole price, rate and commission tier, is frozen at booking. What we charge at the end is what was quoted.

7. Yes, a tutor is suspended automatically when a complaint is upheld. That's Trust & Safety's rule. [improvised] If a tutor is suspended for both reasons, they're reinstated only when both are cleared.

8. [improvised] Yes, payouts are held while a tutor is suspended. The Payouts team collects the tax forms because they need them to pay.

9. [improvised] If the pre-authorisation fails, the booking isn't confirmed. The student has 1 hour to fix their card, otherwise Scheduling cancels the booking and the slot is free again. On a free cancellation the hold is released.

10. [improvised] The commission is reversed too. The tutor only gives back their own share of the refund.

## Critic verdicts, contexts round 2

- Scheduling (merged with Lesson Delivery), Delivery critic | r2 | AGREE | round-1 findings resolved by reply 3.1-3.3; webhook churn in ACL; all appointment endings have one owner; watch Billing<->Scheduling cycle in map
- Scheduling, own critic | r2 | AGREE | merge holds (reply 3.1-3.4, 3.9); Billing must own the single outcome->charge mapping, Scheduling publishes distinguishable endings
- Tutor Profiles | r2 | AGREE | slots stay in Scheduling (reply 3.4); barely a context: generic/supporting, but every alternative owner contradicts a confirmed fact; suggests renaming to Tutor Catalog with Search as its read view; Profiles downstream of Vetting for verified subjects + status

## Facilitator -> user, message 4 (candidate contexts for approval)

Your answers settled both doubts, and every context now has the critic's sign-off. Here's the list for your approval.

**What changed in this round**
- **Lesson delivery merges into Scheduling.** The 10-minute rule is Scheduling's, it doesn't change on its own, Lesson Started isn't a phase change, and technical failures are Scheduling's call. The provider's webhook churn goes into a translation layer (ACL) towards the video provider.
- **Free slots stay in Scheduling.** "Never double-book, first booking wins, a booked slot can't be withdrawn" means slot state and booking must be decided in one place.
- **Tutor Profiles becomes Tutor Catalog**, with search as its read-only view. It's small (one rule, no owning team), so it's a supporting/generic context, but every other owner would contradict something you confirmed.

**The 8 bounded contexts**

| # | Context | The one question it answers | Owns (events, decisions) | Why it's a boundary |
|---|---|---|---|---|
| 1 | **Tutor Vetting** (Trust & Safety) | May this tutor teach on TutorHub? | Tutor Applied, Identity Documents Uploaded, Qualifications Verified, Tutor Approved, Qualification Expired, Qualification Renewed, Tutor Suspended (reason: expired qualification / complaint upheld), Tutor Reinstated. Country document rules, which qualifications count, automatic suspension on Complaint Upheld, reinstated only when all reasons are cleared. | Confirmed pivot *Tutor Approved*; H3: the single owner of "may teach" (your answer: "ONE place, owned by Trust & Safety"). |
| 2 | **Tutor Catalog** (tutor self-service) | What does each tutor offer, and which tutors can a student find? | Tutor Profile Published, Hourly Rate Set/Changed; only subjects Trust & Safety verified; EUR rate; hides suspended tutors (reflects Vetting's decision); search view over profile + rating + free slots. | Owned far side of the *Tutor Approved* pivot; changes for the tutor's reasons. Critic: supporting at most. |
| 3 | **Scheduling** (Scheduling team) | What is the state of each lesson appointment, from booking to how it ended? | Slots, Lesson Requested, Lesson Booked, Booking Confirmed/Expired, Rescheduled, Cancelled (on time / late / by platform), Video Room Opened, Lesson Started, Lesson Ended, No-show Recorded, Lesson Failed Technically, reminder (mechanism). No double booking; 24h free cancellation; max 2 reschedules; 1 h to fix a failed card; 10-minute no-show; technical failure = free reschedule; cancels a suspended tutor's future lessons. | Confirmed pivot *Lesson Booked*; the shopping side has no rules of its own (H3); delivery merged (H3, same owner and rules). |
| 4 | **Pricing** (Finance's rules) | What does this lesson cost, and what is the platform's commission? | Lesson Price Quoted (whole price frozen at booking), late-cancellation fee (50%), commission 15% / 10% (rating >= 4.8 and >= 100 ended lessons). | H3: one calculation for quote, fee and charge; changes every few months, independently of booking. |
| 5 | **Billing** (Billing team) | Which charge applies to how this lesson ended, and has it been collected (or reversed)? | Card Pre-authorized / Failed / Released, Student Charged, Late Cancellation Fee Charged, Payment Failed, Lesson Marked Unpaid, Platform Commission Deducted, Lesson Earnings Determined, Refund Issued. Charge within 1 h, retry twice, then unpaid + email; tutor no-show = no charge; refunds reverse the commission too. Also owns the student's payment card. | Owner of the far side of the *appointment ended* hand-off; own rules (H3 "who owns the rules"). |
| 6 | **Payouts** (Payouts team) | How much do we owe each tutor, and has it been paid out? | Tutor balance, Weekly Payout Sent, Payout Failed, Payout Retried (next Monday), Payout Withheld (< 20 EUR, missing tax forms, tutor suspended), refund deducted from balance (may go negative); collects tax forms. | H2 lane on its own Monday clock; owns the balance (your answer). |
| 7 | **Reviews** (Community team) | What do students think of this tutor? | Review Requested, Review Submitted (1-5 + comment, within 14 days), Tutor Rating Updated (average of last 50). | H2 lane (14-day window), own team and rules; feeds Pricing's tier and the catalog. |
| 8 | **Complaints** (Complaints team) | Is this complaint justified, and what remedy follows? | Complaint Filed, Complaint Investigated, Complaint Rejected, Complaint Upheld, Refund Decided; also no-show disputes. Doesn't suspend, doesn't move money. | H2 lane (anytime, weeks later), own team; H3: suspension stays with Vetting, money with Billing. |

**External systems (not designed by us):** Stripe (cards, payouts via Stripe Connect), the video provider.
**Not contexts:** search (a view inside Tutor Catalog), the lesson reminder (a mechanism), student registration (a generic account, see assumption A1).

**Open assumptions**
- A1. Student Registered is handled by a generic account/identity component we don't design; the payment card belongs to Billing. Correct me if student accounts have rules.
- A2. "Lesson took place" (both joined) is what counts towards the 100-lesson commission tier.

**Suggested board changes:** add Qualification Renewed, Tutor Reinstated, Complaint Upheld / Rejected, Refund Decided, Booking Confirmed / Expired, Pre-authorization Failed / Released, Payment Failed, Lesson Marked Unpaid, Lesson Failed Technically, Payout Withheld, Payout Retried, Hourly Rate Changed; rename Tutor Earnings Credited -> **Lesson Earnings Determined** (Payouts owns the balance), Late Cancellation Fee Applied -> **Late Cancellation Fee Charged**, and split Lesson Cancelled into on time / late / by platform; merge the two Tutor Suspended stickies into one event with a reason.

**Please:**
1. Do you approve these 8 contexts (and assumptions A1-A2)?
2. Do you want a context map in Context Mapper (.cml) format?

## Expert -> facilitator, reply 4 (approval)

I approve the 8 contexts and both assumptions, and yes, please make the CML map.

1. Yes, I approve the 8 contexts, and the names are fine. A1 is right: student accounts have no rules of their own, and the card is Billing's. A2 is right too: only lessons that took place count toward the tier. Go ahead with the board changes.

2. Yes, I want a context map in CML. Call the system TutorHub. Please cover these processes: (1) Tutor onboarding, (2) Book and take a lesson, (3) Weekly payout, (4) Complaint to refund and suspension. At the end I'd also like the canvases for each context.

## Facilitator -> user, message 5 (map: process walkthrough and direction questions)

Thanks, the 8 contexts are approved. I walked your four processes through them. Most hand-offs are clear, but a few decide which side adapts to which, and two of them would otherwise create a cycle (A depends on B and B on A), which the map rules don't allow. I need your answers on those.

**How I walked the processes (draft)**
- **Tutor onboarding:** Tutor Vetting approves and publishes the verified subjects -> Tutor Catalog lets the tutor publish a profile and rate -> Scheduling lets the tutor publish free slots.
- **Book and take a lesson:** search in Tutor Catalog (shows free slots from Scheduling and the rating from Reviews) -> request and book in Scheduling -> Pricing fixes the price at booking -> Billing pre-authorises the card -> the lesson runs in Scheduling (video provider behind a translation layer) -> Scheduling publishes how it ended -> Billing charges -> Payouts and Reviews react.
- **Weekly payout:** Billing reports each lesson's earnings and refunds -> Payouts keeps the balance and pays on Monday via Stripe Connect, holding it while Tutor Vetting says the tutor is suspended.
- **Complaint to refund and suspension:** Complaints investigates -> refund through Billing -> upheld complaint to Tutor Vetting -> Vetting suspends -> Catalog hides, Scheduling cancels future lessons, Payouts holds.

**Questions** (each decides a relationship's direction)
1. **The price quote vs the lesson count.** Pricing needs Scheduling's count of lessons that took place (for the 10% tier). If Scheduling *also* asked Pricing for the quote when booking, the two would depend on each other. Proposal: the student app shows the quote by asking Pricing directly, and Pricing fixes the lesson's price when it sees Lesson Booked; Scheduling itself never needs the price (none of its rules use it). Is that acceptable, or does the Scheduling team need the price inside booking for some rule? *Decides whether Scheduling is upstream of Pricing.*
2. **Pre-authorisation result.** Billing pre-authorises after Lesson Booked; Scheduling confirms the booking or cancels it after 1 hour. Since Billing already depends on Scheduling's lesson outcomes, I propose that Scheduling offers a small booking API ("payment guaranteed for booking B" / "payment guarantee failed for booking B") that Billing calls, so Billing adapts to Scheduling's language and not the other way round. Would the Billing team accept that? *Decides whether Scheduling stays upstream of Billing.*
3. **Upheld complaint -> suspension.** Which team adapts to which: does Trust & Safety listen to Complaints' "complaint upheld against tutor X" and translate it, or does Trust & Safety offer an intake ("report an upheld complaint") that Complaints calls? *Decides the Complaints/Vetting direction.*
4. **Refunds.** Does Billing offer the Complaints team a refund service ("refund lesson L, amount A", plus "what was charged for lesson L") that Complaints calls? Or does Billing listen for Complaints' "refund decided" events? *Decides the Billing/Complaints direction.*
5. **Evidence for complaints.** Does the Complaints team look at the lesson's record in Scheduling (did it take place, who didn't show) when investigating, for example a disputed no-show? *Decides whether Complaints depends on Scheduling. Combined with question 3 it can create a loop, so I'd rather ask than guess.*
6. **Internal teams and external providers.** All eight contexts are run by internal teams working towards one product, so I'll model their links as upstream/downstream with the downstream translating (ACL), and the upstream offering a documented API or events. For Stripe and the video provider: do we wrap them in our own translation layer, or take their models as-is? (You said the video provider's webhook model is messy, so I'd wrap that one for sure.) *Decides ACL vs Conformist towards the external systems.*

## Expert -> facilitator, reply 5

Here are my answers on the relationship directions.

1. No, the Scheduling team needs the price from Pricing. The quote is part of booking: the student requests a slot, sees the quote and books, and it all happens in one booking flow that Scheduling owns.

2. [improvised] Yes, the Billing team is fine with that. They'd report "payment guaranteed" or "guarantee failed" for the booking.

3. Trust & Safety listens. Complaints only says "complaint upheld against tutor X" and doesn't want to know anything about suspension. Trust & Safety turns that into a suspension under their own rule.

4. [improvised] Billing offers the service. Complaints calls "refund lesson L, amount A" and can ask what was charged for the lesson. Billing does the reversal because they know how.

5. [improvised] Yes. For a disputed no-show they look at the lesson's record: did it take place, and who joined and when.

6. Stripe's API is an industry standard, so we're happy to follow it as-is in the payment code. Wrap the video provider's model: it's messy and they've changed it twice this year.

## Facilitator -> user, message 6 (breaking two cycles)

Thanks. Your answers fix most directions, but they also expose two loops that I have to break before I can draw the map. Here's what I tried; each fix needs a yes/no from you.

**Loop 1: Pricing <-> Scheduling (and via Reviews and Tutor Catalog)**
Scheduling asks Pricing for the quote (your answer 1), so Pricing is upstream of Scheduling. But Pricing's 10% tier needs the count of lessons that took place (Scheduling's fact) and the rating (Reviews, which only learns about a lesson from Scheduling), and the price needs the hourly rate (Tutor Catalog, whose search shows Scheduling's free slots). Every one of those feeds loops back to Scheduling.
- Tried: dropping Scheduling's quote call. Rejected by your answer 1.
- Tried: getting the count from Billing or Reviews instead. Still loops, because both learn about lessons from Scheduling.
- **Proposed fix:** Pricing (Finance's calculator) offers one documented API in its own language, and everyone *feeds* it: Tutor Catalog registers the tutor's hourly rate when the tutor changes it, Reviews registers the tutor's new rating, Scheduling records "lesson took place" and asks for quotes. The tutor still sets the rate in the catalog, and Reviews still owns the rating; they just report them to Pricing in Pricing's terms. Pricing then depends on nobody.

**Loop 2: Trust & Safety -> Scheduling -> Complaints -> Trust & Safety**
Trust & Safety's suspension cancels a tutor's future lessons in Scheduling; Complaints reads Scheduling's lesson record (your answer 5); Trust & Safety listens to Complaints (your answer 3). Those last two are your facts, so the only link left to flip is the first one.
- **Proposed fix:** when Trust & Safety suspends a tutor, it calls Scheduling's API "close tutor X's calendar" (cancel future lessons, no new bookings) and "reopen tutor X's calendar" on reinstatement, the same way Billing calls Scheduling's booking API. Trust & Safety still decides; Scheduling just offers the command in its own language.

**Questions**
1. Would Tutor Catalog, Reviews and Scheduling accept reporting the rate, the rating and lessons that took place to Pricing's API, so that Pricing (Finance) depends on no other team? *Decides whether Pricing can be upstream of everyone.*
2. Would Trust & Safety accept calling Scheduling's "close/reopen tutor's calendar" API on suspension and reinstatement, instead of Scheduling listening for suspensions? *Decides whether Scheduling is upstream of Trust & Safety, which breaks loop 2.*

If you say no to either, I'll keep that loop, record why each flip was rejected, and mark it in the map.

## Expert -> facilitator, reply 6

Yes to both fixes. Here are the details.

1. [improvised] Yes. Finance would like that, because their calculator only needs the rate, the rating and the lesson count, in their own terms. The tutor still sets the rate in the catalog and Community still owns the rating; they just report them.

2. [improvised] Yes. Trust & Safety is fine with calling "close tutor X's calendar" when they suspend and "reopen" when they reinstate. All Scheduling needs is to know whether a tutor may teach, and a closed calendar tells them that.

## Critic verdicts, map round 1 (one per process)

- Map + Weekly payout | r1 | AGREE | all hand-offs carried, Payouts in no cycle, CF to Stripe justified; notes: fix citation (reply 5.6), name Payouts events; non-blocking Q: who knows tutor's country/Connect account
- Map + Complaint to refund and suspension | r1 | AGREE | every step owned, no cycle (cycle B flip accepted), Refunds direction right; non-blocking Qs: refund failure, tutor-filed remedies, cap on amount A
- Map + Tutor onboarding | r1 | AGREE | all hand-offs carried, graph acyclic; note: 'open calendar' on approval is facilitator's inference (expert confirmed close/reopen) -> confirm with expert or mark in CML; fix Stripe citation
- Map + Book and take a lesson | r1 | AGREE | every step owned, directions follow language, acyclic; notes: say FrozenLessonPrice travels inside Lesson Booked; split LessonOutcomes (PL) from PaymentGuarantee (OHS); search 'price' = hourly rate assumed

## Facilitator -> user, message 7 (one gap before the final map check)

Thanks, both loops are broken and the map validates. The four process critics all agreed. One small gap before the final check, so the map has no unconfirmed links:

1. You confirmed that Trust & Safety calls "close tutor X's calendar" on suspension and "reopen" on reinstatement. Does it also call "open tutor X's calendar" when a tutor is first approved, so tutors can publish slots? *If not, Scheduling would have to listen for Tutor Approved, which brings back the Trust & Safety -> Scheduling -> Complaints -> Trust & Safety loop.*

## Expert -> facilitator, reply 7

[improvised] Yes. When Trust & Safety approves a tutor, it also calls "open tutor X's calendar" so the tutor can start publishing slots. It's the same call they'd use to reopen a calendar after a suspension.

## Critic verdict, map final pass

- Map final pass (all 4 processes) | r2 | AGREE | acyclic, no unconfirmed links, CF justified; notes: Scheduling vision should say open/close/reopen calendar; two Scheduling->Billing links are one U/D pair with two channels

## Facilitator -> user, message 8 (context map for approval)

The context map is ready: `src/main/cml/TutorHub-Context-Map.cml`. It parses in Context Mapper with 0 errors (10 contexts including Stripe and the video provider, 19 relationships). Independent critics walked all four of your processes through it and agreed, including one final pass over all of them together.

**Relationships** (upstream -> downstream; internal links are upstream API/events + downstream translation layer)

| Upstream | Downstream | What flows | Why this direction |
|---|---|---|---|
| Stripe | Billing, Payouts | cards; Connect payouts | We follow Stripe as-is (Conformist): "industry standard" (you) |
| VideoProvider | Scheduling | room and attendance webhooks | Wrapped in a translation layer: messy, changed twice |
| Pricing | Scheduling | quote (frozen at booking); "lesson took place" | Pricing's own API; Scheduling needs the quote in booking |
| Pricing | TutorCatalog, Reviews | rate and rating registered with Pricing | Pricing's API, your "yes" to the feeding fix |
| Pricing | Billing | frozen price breakdown (travels inside Lesson Booked) | Charge = quote |
| Scheduling | Billing | lesson outcomes (events) + "payment guaranteed / failed" (Billing calls) | Billing adapts to Scheduling |
| Scheduling | TutorVetting | open / close / reopen tutor calendar (T&S calls) | Your "yes"; breaks the T&S loop |
| Scheduling | Complaints | lesson record (who joined, when) | Evidence for disputes |
| Scheduling | Reviews, TutorCatalog | lesson took place; free slots | |
| Billing | Complaints | refund lesson L amount A; what was charged | Billing offers the service |
| Billing | Payouts | lesson earnings, refunds (tutor's share) | Payouts owns the balance |
| Complaints | TutorVetting | Complaint Upheld | T&S listens |
| TutorVetting | TutorCatalog, Payouts | approved (+ verified subjects), expired, suspended, reinstated | One owner of "may teach" |
| Reviews | TutorCatalog | rating | |

**Validator findings:** only two "conformist" notices (Billing and Payouts follow Stripe), both justified by your answer. No cycles, no shared kernel, no context left without a link.

**Loops I broke (with your answers):** Pricing <-> Scheduling (Pricing's feeding API), Trust & Safety -> Scheduling -> Complaints -> Trust & Safety (calendar API), Billing <-> Scheduling (payment-guarantee API).

**Open assumptions:** the "price" shown in search is the tutor's hourly rate, not a Pricing quote. Smaller questions for the canvases (refund failures, tutor-filed complaints, who knows a tutor's country for tax forms) are listed in the report.

Do you approve the map? If yes, I'll create the eight canvases next.

## Expert -> facilitator, reply 8 (map approval)

Yes, I approve the map. Your assumption is right: the price in search is just the tutor's hourly rate, because search shows what tutors publish. Please go ahead with the eight canvases.

## Facilitator -> user, final message

Discovery is done, and the map and canvases are ready for you to review.

**Files**
- Report (heuristic trail, Q&A, critic log, validator output): `docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md`
- Context map: `src/main/cml/TutorHub-Context-Map.cml`. It opens in Context Mapper, and the validator reports 0 errors.
- Canvases: `docs/bounded-contexts/tutorhub/<context>/canvas.md` for tutorvetting, tutorcatalog, scheduling, pricing, billing, payouts, reviews and complaints. They are canvases only, with no plan or code.

**Size:** 8 contexts plus Stripe and the video provider, connected by 19 relationships. No cycles, no shared kernel. Billing and Payouts follow Stripe as-is, because you called it an industry standard.

**Critic rounds that changed something**
- Lesson Delivery merged into Scheduling. The webhook churn is a translation-layer concern.
- Free slots stayed in Scheduling, so no slot is ever double-booked. Tutor Profiles became Tutor Catalog, with search as its view.
- The map critics split Scheduling -> Billing into lesson outcomes and payment-guarantee commands. They also had me confirm the "open calendar" call on approval.
- With your answers, three loops were broken: Pricing/Scheduling, Trust & Safety/Scheduling/Complaints, and Billing/Scheduling.

**Still open**
- Is commission added on top of the rate or deducted from it? How are refunds and the late fee split?
- Is a qualification expiry per subject? Do complaints have deadlines? What happens when a refund fails, or a tutor files a complaint?
- Where does Payouts get a tutor's country and Stripe Connect account?
- Several canvas rules are marked "needs GWT".

Next: review the canvases, then run bounded-context-scaffolder on each context to get its plan.
