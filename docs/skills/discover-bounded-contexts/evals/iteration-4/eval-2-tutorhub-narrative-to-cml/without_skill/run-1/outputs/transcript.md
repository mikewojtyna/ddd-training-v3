# Transcript: TutorHub narrative to context map (baseline, no skill)

## User
I typed up the stickies from our TutorHub event storming in /home/goobar/IdeaProjects/ddd-training-v3/.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md. Can you figure out where the bounded context boundaries are and then draw me a context map I can open in Context Mapper?

## Facilitator -> Expert (round 1)
Thanks for the typed-up stickies. Before I draw boundaries I have a few questions where the board is ambiguous:

1. "Tutor Suspended" appears twice: once after a complaint investigation and once after "Qualification Expired". Is it the same suspension done by the same people/team, and what does a suspension actually do (hide the profile from search, cancel future lessons, stop payouts)?
2. Who verifies identity documents and qualifications, and who keeps track of qualification expiry? Is this the same team that later handles complaints?
3. Lesson price: how is "Lesson Price Quoted" calculated? Is it just the tutor's hourly rate times duration, or are there platform rules (discounts, fees, commission shown to student)? Who owns the late cancellation fee rules?
4. Money flow: the card is pre-authorized at booking and the student is charged after the lesson. What happens to the charge on a no-show (student or tutor) and on a cancellation? Is the commission a fixed percentage?
5. Payouts: "every Monday no matter what happens with lessons" - does a later complaint or refund claw back money from a tutor's earnings, or is that absorbed by the platform?
6. Refund Issued: who decides a refund (support after a complaint? automatic on tutor no-show?), and who actually executes it?
7. Reviews and ratings: who uses the tutor rating (search ranking, suspension decisions)? Can a review lead to a complaint or vice versa?
8. Students: does "Student Registered" / "Payment Card Added" belong to a general accounts area, or is card handling part of payments with Stripe?
9. Are there separate teams/people around this board (e.g. tutor onboarding team, support / trust & safety, finance) whose language differs? Any words that mean different things to different groups (e.g. "lesson", "booking", "session")?

## Expert (round 1)
1. Same outcome, two different reasons. One is an expired qualification, which is a Trust & Safety rule: the tutor is suspended until they renew. The other is an upheld complaint. Right now both teams suspend tutors on their own, and it's a mess. A tutor suspended by Complaints got re-activated by Trust & Safety after renewing a certificate. We want ONE place that decides whether a tutor may teach, and Trust & Safety should own it. Complaints should only say "complaint upheld against tutor X". A suspended tutor's profile is hidden from search and they can't receive bookings. [improvised: lessons already booked aren't cancelled automatically today, and earnings already credited are still paid out.]

2. Trust & Safety does the application, the ID upload, the qualification checks and the approval. They have their own rules for which documents each country needs and which qualifications count, and approval happens once. They also track qualification expiry. Complaints is a different team.

3. The quote is the tutor's hourly rate × duration + platform commission. The tutor sets their own rate and can change it any time, but the rate valid at booking time applies. Commission is 15%, or 10% for tutors with a rating of 4.8 or more and at least 100 lessons. The business/finance people set the commission rules and change them every few months, independently of booking. The Scheduling team owns the cancellation rules: free up to 24h before the lesson, then a late cancellation fee of 50% of the lesson price, and a maximum of 2 reschedules per lesson. The price, the fee and the final charge all use the same calculation.

4. The pre-authorization is just Stripe holding the money. If the tutor no-shows, the student isn't charged. If the student no-shows, they pay in full. A cancellation more than 24h before is free; after that the student pays the 50% fee. No-show means someone didn't join within 10 minutes, and we decide that ourselves from the video provider's webhooks. Billing builds the final charge, charges the card within 1 hour, retries twice, then marks it "unpaid" and emails the student. Commission is 15% or 10%, as above.

5. Payouts run on their own clock. Every Monday they pay out accumulated earnings via Stripe Connect. The minimum is 20 EUR, a failed payout is retried the next Monday, and tutors in some countries need tax forms first. [improvised: a refund after the charge is deducted from the tutor's future earnings balance. Already paid-out money isn't pulled back from their bank.]

6. The Complaints team decides refunds and Billing executes them. Complaints just says "refund lesson L, amount A", and Billing knows how to reverse the charge. [improvised: a tutor no-show isn't a refund case, because the student was never charged.]

7. Students rate a lesson 1-5 with a comment, up to 14 days after it. A tutor's rating is the average of their last 50 reviews. A small Community team owns reviews. Pricing reads the rating for the commission tier, and search shows it and lets you filter by it. [improvised: ratings don't drive suspensions. A review and a complaint are separate things; a bad review doesn't open a complaint and vice versa.]

8. [improvised] Registration is just a student account. Adding a card is really Stripe storing it, and we only keep the reference that the payment side uses for pre-authorization and charges.

9. Yes. The teams are Trust & Safety, Scheduling, Billing, Payouts, Community, Complaints, and Finance (which owns the pricing and commission rules). They're all in the same company and work on the same product. Stripe and the video provider are external. [improvised: "lesson" means different things to different teams. To Scheduling it's an appointment in the calendar. To Billing it's a chargeable item. To Complaints it's the thing being complained about. "Session" is mostly the video provider's word for the room.] Nobody wants a shared database or shared code library again; the last shared "common" module was a disaster.

## Facilitator -> Expert (round 2)
Thanks, that is very helpful. Here is my first draft of the boundaries, followed by a few remaining questions.

Draft bounded contexts:
- Tutor Vetting (Trust & Safety): application, ID documents, qualification checks, approval, qualification expiry, and the single "may this tutor teach?" decision (suspend / reinstate). Complaints only reports "complaint upheld against tutor X".
- Tutor Catalog: tutor profile, hourly rate, availability slots, search (hides non-eligible tutors, shows ratings).
- Student Accounts: student registration.
- Scheduling: lesson requested, booked, reminders, reschedules (max 2), cancellations and the 24h rule.
- Lesson Delivery: video room, lesson started/ended, no-show detection (10 minutes) from the video provider's webhooks.
- Pricing (Finance): one calculation for quote, late cancellation fee and final charge; commission rules (15% / 10% tier).
- Billing: card reference, pre-authorization, final charge within 1h, retries, unpaid, executing refunds.
- Payouts: tutor earnings balance, weekly Monday payouts via Stripe Connect, 20 EUR minimum, tax forms, failed payout retry, deducting refunds from future earnings.
- Reviews (Community): review requests, reviews within 14 days, rating = average of last 50.
- Complaints: complaint filed, investigated, upheld, refund decision.
- External: Stripe, Video provider.

Questions:
1. Who owns the tutor profile, hourly rate, availability slots and search? Is that its own team, or part of Scheduling or Trust & Safety?
2. Who owns the video room, "lesson started/ended" and no-show detection - Scheduling or someone else?
3. "Platform Commission Deducted" and "Tutor Earnings Credited": are these done by Billing when the student is charged, or by Payouts? Where does the tutor's earnings balance live?
4. Where does the Stripe card reference live: student accounts or Billing?
5. Does anything look wrong or missing in my draft split?

## Expert (round 2)
Domain expert answers to your draft questions (TutorHub product owner):

1. The tutor publishes the profile and sets the hourly rate once Trust & Safety approves them. They can change the rate any time, and the rate valid at booking time applies. Search just finds tutors by subject, price, rating and free slots. It has no rules of its own and only shows what others publish. [improvised: a small marketplace/product team maintains the profile pages and search. Tutors keep their free slots in their calendar, and Scheduling books against those slots.]

2. The third-party video provider opens the room 5 minutes before the start, and Lesson Started / Ended come from their webhooks. We decide no-shows ourselves: someone didn't join within 10 minutes. The webhook model is messy, and they've changed it twice this year. [improvised: a small team looks after the video integration and the lesson-day facts. Scheduling only cares whether the lesson happened, was a no-show or was cancelled.]

3. Billing does both. When it builds the final charge, it charges the card, deducts the commission and credits the tutor's earnings. Payouts then pays out the accumulated earnings every Monday. [improvised: Payouts keeps the earnings balance it pays from. Billing just tells it "credit tutor X amount Y" or "deduct Y" for a refund.]

4. [improvised] It sits with Billing, the payment side. The student account only knows the student has a card on file.

5. From a business point of view, that list matches how we work, so I'm fine with it. Our teams are Trust & Safety, Scheduling, Billing, Payouts, Community, Complaints and Finance (pricing rules). Nobody wants a shared database or shared code library between teams.

## Facilitator -> Expert (round 3)
Thanks. I've worked out the relationships between these contexts. To avoid circular dependencies between teams, I'd like to confirm a few decisions:

1. Hourly rate: the tutor sets it, but I'd keep the rate (and the history of which rate was valid when) in Pricing, next to the commission rules, because "the rate valid at booking time" is a pricing rule. The profile page and search would only display it. Does that work for you, or does the rate really belong to the profile?
2. Lesson Delivery would offer Scheduling a "video room" service: Scheduling asks it to set up, move or close a room for a booked lesson. Lesson Delivery then publishes "lesson happened / student no-show / tutor no-show", and Scheduling, Billing, Reviews and Pricing (for the 100-lessons count) listen. OK?
3. Billing would offer Scheduling a service: pre-authorize at booking, and charge the late cancellation fee when Scheduling reports a late cancellation. Billing charges after the lesson based on the Lesson Delivery outcome. If a pre-authorization fails, the booking doesn't go through. Is that right?
4. Complaints publishes two decisions, "complaint upheld against tutor X" (Trust & Safety reacts) and "refund lesson L, amount A" (Billing executes it). Complaints doesn't need to pull data from Billing; the complaint just references the lesson. OK?
5. Is a refund ever triggered by anything other than a complaint?

## Expert (round 3)
Domain expert answers on your five relationship decisions (TutorHub product owner):

1. That works for me. The tutor sets the rate and can change it any time, and the rate valid at booking time is what counts. The same price calculation is used for the quote, the late cancellation fee and the final charge, so I'm fine with the rate living next to it. The profile page and search only need to show the current rate.

2. OK. In business terms: the provider opens the room 5 minutes before the start, and we decide the outcome ourselves from its webhooks (no-show = didn't join within 10 minutes). After that, money things can start. If the tutor didn't show, the student isn't charged. If the student didn't show, they pay in full. Pricing does need the lesson count for the 10% tier, which is a rating of 4.8 or more and at least 100 lessons.

3. Yes, that's right. The card is pre-authorized right after booking. After a late cancellation (later than 24h before the lesson), Billing charges 50% of the lesson price. After the lesson or a no-show, it charges within 1 hour, retries twice, then marks the charge "unpaid" and emails the student. [improvised: if the pre-authorization fails, the booking doesn't go through and the student is asked for another card.]

4. OK. Complaints decides the refund and its amount, and Billing knows how to reverse the charge. The complaint only references the lesson.

5. [improvised] No, only complaints. A tutor no-show doesn't need a refund because the student is never charged. A free cancellation (more than 24h before) just releases the pre-authorization.
