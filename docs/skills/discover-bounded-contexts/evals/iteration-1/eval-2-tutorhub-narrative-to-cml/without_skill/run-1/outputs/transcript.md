# Transcript: TutorHub narrative to CML (baseline, no skill)

## User

I typed up the stickies from our TutorHub event storming in /home/goobar/IdeaProjects/ddd-training-v3/.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md. Can you figure out where the bounded context boundaries are and then draw me a context map I can open in Context Mapper?

## Assistant to user (message 1)

I've read through your TutorHub stickies. Before I draw boundaries, I have a few questions where the board is ambiguous:

1. "Tutor Suspended" appears twice: once after a complaint investigation and once after "Qualification Expired". Is that the same suspension, decided by the same people with the same rules? And who notices that a qualification expired?
2. "Hourly Rate Set" vs "Lesson Price Quoted": who sets the hourly rate (tutor or platform), and is the quoted lesson price just rate x duration, or are there other rules (discounts, platform fees, currency)? Who decides the late cancellation fee?
3. Money: is the student charged after the lesson ends (and on a no-show)? Who issues refunds after a complaint, and does a refund claw back the tutor's earnings even if the Monday payout has already gone out?
4. Is "Tutor Approved" a gate - i.e. can a tutor only publish a profile, set rates and accept bookings once approved, and does a suspension hide the profile / block new bookings?
5. Do reviews and ratings affect anything else (search ranking, suspension), or are they just shown on the profile?
6. Are there different teams/people in the company owning these areas (e.g. vetting team, support/trust & safety, finance)?

## Expert reply 1

1. No, and that's our problem right now. There are two reasons for a suspension. One is an expired qualification: Trust & Safety's rule is that the tutor stays suspended until they renew it, and Trust & Safety are the ones who notice the expiry. The other is an upheld complaint. Today both teams suspend tutors on their own, and once Trust & Safety re-activated a tutor that Complaints had suspended, just because the tutor renewed a certificate. We want ONE place that decides whether a tutor may teach, owned by Trust & Safety. Complaints should only report "complaint upheld against tutor X".

2. The tutor sets their own hourly rate and can change it any time. The rate valid at booking time applies. The quoted price is the tutor's rate x duration plus our platform commission. The commission is 15%, or 10% for tutors with a rating of 4.8 or more and at least 100 lessons. The business/finance people set those commission rules and change them every few months, independently of booking. There are no other discounts, and everything is in EUR [improvised]. The late cancellation fee is a Scheduling team rule: cancelling is free up to 24h before the lesson, and after that the fee is 50% of the lesson price. The fee uses the same price calculation as the quote and the final charge.

3. Yes. The card is pre-authorised right after booking (that's just Stripe holding the money). Billing charges it after Lesson Ended, a no-show or a late cancellation, and they do it within 1 hour. They retry twice, then mark the lesson "unpaid" and email the student. If the student no-shows, they pay in full. If the tutor no-shows, the student isn't charged. Complaints decides on refunds ("refund lesson L, amount A") and Billing carries them out by reversing the charge. If the Monday payout already went out, we deduct the amount from the tutor's next payout [improvised].

4. Yes. Trust & Safety approves a tutor once, and only after that can the tutor publish a profile, set rates and get bookings. A suspended tutor's profile is hidden from search and they can't receive new bookings.

5. Students rate a lesson 1-5 with a comment, up to 14 days after it. A tutor's rating is the average of their last 50 reviews. Pricing uses the rating for the commission tier, and search lets students filter by rating. Ratings don't cause suspensions; only upheld complaints and expired qualifications do.

6. Yes, all in the same company, working on the same product:
   - Trust & Safety: vetting, approval, and whether a tutor may teach
   - Scheduling: booking, cancellation and reschedule rules
   - Finance/business: the pricing and commission rules
   - Billing: charges and refunds
   - Payouts: Monday payouts via Stripe Connect
   - Community: reviews
   - Complaints

## Assistant to user (message 2)

Thanks, that's very helpful. A few follow-ups so I put the remaining stickies in the right place:

1. Availability slots: who owns them - is it part of the tutor's profile (managed with the rate), or does Scheduling own the calendar and mark slots as taken when a lesson is booked or rescheduled?
2. "Video Room Opened", "Lesson Started", "Lesson Ended", "No-show Recorded": who owns the actual lesson session, and how is a no-show detected (e.g. from video provider attendance, after X minutes)? Is that a separate team from Scheduling?
3. "Platform Commission Deducted" and "Tutor Earnings Credited": does Payouts just take the commission amount that Pricing computed at quote time, or recompute it? And for the 10% tier, where does the "at least 100 lessons" count come from?
4. "Student Registered" and "Payment Card Added": who owns student accounts? Is the card part of Billing (Stripe customer) or part of the student account?
5. Who sends "Lesson Reminder Sent" and "Review Requested" - Scheduling and Community respectively, or a shared notifications team?

## Expert reply 2

Here are the domain expert's answers to your 5 TutorHub follow-ups. Anything marked [improvised] isn't on my answer sheet; it's the most plausible answer that fits the facts.

1. Scheduling owns the calendar. Tutors enter their free hours there, and Scheduling marks a slot taken when a lesson is booked and frees it when the lesson is cancelled or rescheduled [improvised]. The rate stays with the tutor's profile. Search only reads the profile, the rate, the rating and the free slots from whoever owns each of them.

2. The video room itself is run by a third-party provider. It opens the room 5 minutes before the start, and "Lesson Started" and "Lesson Ended" come from the provider's webhooks. Their webhook model is messy and they've changed it twice this year. We decide no-shows ourselves from those webhooks: a no-show is the student or the tutor not joining within 10 minutes. If the student no-shows, they pay in full. If the tutor no-shows, the student isn't charged. The team running the lesson session sits next to Scheduling but is separate from it [improvised]. Lesson Ended means the lesson is over and the money side can start.

3. It's Billing, not Payouts. Billing builds the final charge, charges the card, deducts the commission and credits the tutor's earnings. Payouts only pays out the accumulated earnings every Monday (minimum 20 EUR, failed payouts retried next Monday, tax forms required in some countries). Billing uses the same Pricing calculation as the quote, with the rate that was valid at booking time, so the numbers match the quote [improvised on whether it's stored or recomputed]. The "100 lessons" count is the tutor's number of completed lessons, taken from the lesson facts [improvised]. The rating comes from Community.

4. Students have a simple account of their own, mostly login and profile, and it's not owned by any of the teams above [improvised]. The card is a Stripe customer/payment method, so it belongs on the payment/Billing side and not in the student account [improvised]. Stripe's API is an industry standard and we're happy to follow it.

5. Each team decides when its own message goes out: Scheduling sends the lesson reminder and Community sends the review request. A small shared email/notification service does the actual sending, but it has no business rules [improvised].

## Assistant to user (message 3)

Thanks. Here is my proposed split and two design decisions I'd like you to confirm or correct.

Bounded contexts:
- Tutor Vetting (Trust & Safety): application, documents, qualification checks, approval, qualification expiry, and the ONE decision whether a tutor may teach (suspend / reinstate, considering both expired qualifications and upheld complaints).
- Tutor Catalog: tutor profile, hourly rate (with history, so the rate valid at booking time can be looked up), and tutor search (reads free slots, rating and eligibility).
- Student Accounts: login and profile (generic).
- Scheduling: calendar/free slots, lesson requests, bookings, reschedules, cancellations, the late-cancellation rule, reminders.
- Lesson Delivery: the lesson session, video room via the provider (behind an anti-corruption layer because of their messy webhooks), started/ended, no-show detection, completed-lesson count.
- Pricing (Finance): price quote = rate x duration + commission tier (15% / 10%).
- Billing: card (Stripe), pre-authorisation, final charge, retries/unpaid, commission deduction, tutor earnings credit, refunds (and earnings debit if a refund comes after a payout).
- Payouts: Monday payouts via Stripe Connect, minimum 20 EUR, retries, tax forms.
- Reviews (Community): review requests, reviews, tutor rating.
- Complaints: filing, investigation, decisions "refund lesson L, amount A" and "complaint upheld against tutor X".
- External: Stripe, Video Provider; plus the shared notification service (no business rules).

Decision A: Scheduling does NOT call Pricing. The booking screen asks Pricing for the quote directly, and Scheduling only says "late cancellation, fee = 50% of lesson price"; Billing turns that into an amount using Pricing. Otherwise we'd get a dependency loop (Scheduling -> Lesson Delivery -> Pricing -> Scheduling).
Decision B: Complaints only stores the lesson id the student complains about; it doesn't subscribe to lesson data. Otherwise there'd be a loop Trust & Safety -> Scheduling -> Lesson Delivery -> Complaints -> Trust & Safety.

Is that right? Anything misplaced?

## Expert reply 3

TutorHub domain expert: the list of contexts is right and Decision B is fine, but Decision A doesn't match how we work.

The list matches how we run the business, and the names are fine with me. Trust & Safety as the one place that decides whether a tutor may teach is exactly what we want. I'd change nothing on the list.

Decision A: Scheduling does need the price. Scheduling owns the booking: the student requests a slot, sees a quote and books, all inside Scheduling. Scheduling also needs the price to apply its late-cancellation rule (50% of the lesson price). We use the same price calculation for the quote, the late fee and the final charge, and Billing also uses it for the charge. Pricing itself only needs the tutor's rate and the tier inputs: the rating from Community and the completed-lesson count. It never needs anything from the booking. If that gives you a loop in your diagram, the fix has to come from somewhere other than taking the price away from Scheduling.

Decision B: fine by me. A complaint just points to the lesson it's about, and the investigators look up what happened in that lesson when they need to [improvised].

## Assistant to user (message 4)

Understood: Scheduling keeps the price. I'll break the loop elsewhere, which means two changes. Please confirm them.

Change 1, Lesson Delivery as a service that Scheduling uses: Lesson Delivery doesn't listen to Scheduling's bookings. Scheduling calls Lesson Delivery's API to set up the session when a lesson is booked ("lesson L, tutor T, student S, at time X"), and calls it again to move or cancel the session on a reschedule or cancellation. Lesson Delivery then runs the room, decides no-shows and publishes Lesson Ended / No-show Recorded, which Billing, Community and Pricing (completed-lesson count) consume. With that, the dependencies go one way: Lesson Delivery -> Pricing -> Scheduling.

Change 2, Tutor Search as its own read-only context: search pulls together profiles and rates, free slots from Scheduling, ratings from Community, and eligibility from Trust & Safety. If search stays inside the tutor-profile context, we get another loop: Profiles -> Pricing (rate) -> Scheduling -> Profiles (free slots). So I'd keep Tutor Profiles (profile + hourly rate) and add a separate Tutor Search that only reads from the others and has no rules of its own beyond filtering and ranking.

Does that still match how you work?

## Expert reply 4

TutorHub domain expert: yes, both changes match how we work.

Change 1: That's fine. Once a lesson is booked it's a real appointment, so Scheduling telling the lesson side "set up lesson L for tutor T and student S at time X", and later "move it" or "cancel it", is how I picture it [improvised on the exact mechanism]. After that, the lesson side runs the room with the video provider, decides no-shows (nobody joined within 10 minutes) and announces that the lesson ended or was a no-show. The money side and reviews then take it from there.

Change 2: Yes. Search has no business rules of its own. It just lets students find tutors by subject, price, rating and free slots, and shows what the other teams publish. Suspended tutors are hidden from it. Simple filtering and sorting is all it does; there are no ranking rules.

With these two changes I'm happy with the list.

## Assistant to user (message 5, final)

See final_message.md (verbatim).

I've drawn the TutorHub context map. Open `src/main/cml/TutorHub-Context-Map.cml` in Context Mapper. It parses with no errors and renders. A PNG is next to it: `TutorHub-Context-Map.png`.

**Bounded contexts (11 of ours, plus Stripe and the video provider):**

| Context | Owner | Stickies |
|---|---|---|
| TutorVetting | Trust & Safety | Tutor Applied, Identity Documents Uploaded, Qualifications Verified, Tutor Approved, Qualification Expired, Tutor Suspended (both) |
| TutorProfiles | | Tutor Profile Published, Hourly Rate Set |
| TutorSearch | | Tutor Searched (read-only) |
| StudentAccounts | | Student Registered |
| Scheduling | Scheduling | Availability Slots Published, Lesson Requested, Lesson Booked, Lesson Reminder Sent, Lesson Rescheduled, Lesson Cancelled |
| LessonDelivery | | Video Room Opened, Lesson Started, Lesson Ended, No-show Recorded |
| Pricing | Finance | Lesson Price Quoted, the commission rules |
| Billing | Billing | Payment Card Added, Card Pre-authorized, Late Cancellation Fee Applied, Student Charged, Platform Commission Deducted, Tutor Earnings Credited, Refund Issued |
| Payouts | Payouts | Weekly Payout Sent, Payout Failed |
| Reviews | Community | Review Requested, Review Submitted, Tutor Rating Updated |
| Complaints | Complaints | Complaint Filed, Complaint Investigated |

There's also a generic **Notifications** service with no rules: it only sends what Scheduling and Reviews tell it to.

**The decisions that shape the map:**
- **Suspension:** only TutorVetting suspends tutors. The two "Tutor Suspended" stickies were two teams suspending on their own, which is how a complaint suspension got undone by a certificate renewal. Now Complaints only publishes "complaint upheld against tutor X". TutorVetting decides whether a tutor may teach, and Scheduling, TutorProfiles and TutorSearch follow that decision.
- **Refunds:** Complaints decides a refund ("refund lesson L, amount A") and Billing carries it out. Billing also deducts commission and credits earnings. Payouts only pays out the balance every Monday, so a refund after a payout comes off the next payout.
- **Pricing:** one calculation is used for the quote, the late-cancellation fee and the final charge, and Scheduling and Billing both use it.
- **No upstream/downstream loops** (checked with `tsort`). Getting there took two changes, both confirmed with you:
  - **Scheduling calls LessonDelivery.** It uses LessonDelivery's API to set up, move or cancel a session. LessonDelivery publishes Lesson Ended and No-show, and Billing, Reviews and Pricing (for the completed-lesson count) use them.
  - **Search is its own read-only context.** If it stayed inside TutorProfiles, there would be a loop: Profiles → Pricing → Scheduling → Profiles.
  - Complaints also keeps only the id of the lesson a complaint is about, without subscribing to lesson data.
- **External systems:**
  - The video provider sits behind an anti-corruption layer in LessonDelivery, because its webhooks are messy and have changed twice this year.
  - We conform to Stripe in Billing and to Stripe Connect in Payouts, since Stripe is an industry standard you're happy to follow.

**Answers that weren't on the board, worth rechecking with the teams:**
- The rate valid at booking time is looked up from a rate history in TutorProfiles.
- The 100-lesson count is completed lessons from LessonDelivery.
- A refund after a payout is deducted from the next payout.
- EUR only.
