# Transcript

## User
I typed up the stickies from our TutorHub event storming in /home/goobar/IdeaProjects/ddd-training-v3/.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md. Can you figure out where the bounded context boundaries are and then draw me a context map I can open in Context Mapper?

## Facilitator -> Expert (message 1)
Thanks for the typed-up stickies. Before I draw boundaries I'd like to check a few things the board doesn't make clear:

1. "Tutor Suspended" appears twice — once after a complaint and once after "Qualification Expired". Is that the same thing (one suspension owned by one team, with two triggers), or two different kinds of suspension? And what does a suspension actually do — hide the profile, cancel future lessons, freeze payouts?
2. Who decides that a tutor is approved and that a qualification has expired — is that the same people/process as tutor onboarding, or a separate compliance/trust team?
3. Booking: in what order do "Lesson Booked" and "Card Pre-authorized" really happen? If pre-authorization fails, is the lesson still booked? And who sets the lesson price — is it simply the tutor's hourly rate, or does the platform compute it (discounts, fees)?
4. "No-show Recorded" and "Late Cancellation Fee Applied": who decides these and what follows (student charged, tutor not paid, etc.)?
5. Money: "Student Charged", "Platform Commission Deducted", "Tutor Earnings Credited", "Weekly Payout Sent", "Payout Failed", "Refund Issued" — is this one finance team/ledger, or are charging students and paying tutors separate concerns? What happens when a payout fails?
6. Reviews and complaints: is "Complaint Filed/Investigated" handled by customer support, and does it only lead to refunds and suspensions? Does the tutor rating feed into search ranking?
7. Are search/discovery of tutors and the tutor's public profile owned by the same team as onboarding, or separate?

## Expert -> Facilitator (reply 1)
1. There are two reasons for a suspension: an expired qualification (a Trust & Safety rule says you're suspended until you renew it) or an upheld complaint. Today both teams suspend tutors on their own, and it's a mess. A tutor suspended by Complaints got re-activated by Trust & Safety after renewing a certificate. We want one place to decide whether a tutor may teach, and Trust & Safety should own it. Complaints should only say "complaint upheld against tutor X". When a tutor is suspended, their profile is hidden from search and they can't receive bookings. As far as I know we don't freeze earnings that are already due. [improvised]

2. Trust & Safety handles the application, the ID upload, the qualification checks and the approval. They have their own rules, like which documents each country needs and which qualifications count. Approval happens once. The "expired qualification means suspended" rule is theirs too.

3. The student requests a slot, sees a quote and books. The card pre-authorization comes right after the booking; it's just Stripe holding the money. If the hold fails, the booking doesn't stand and the slot is freed again. [improvised] The price is the tutor's hourly rate (the one valid at booking time) times the duration, plus the platform commission. The commission is 15%, or 10% for tutors with a rating of 4.8 or more and at least 100 lessons. The business/finance people set those commission rules and change them every few months, independently of booking. The same calculation is used for the quote, the late cancellation fee and the final charge.

4. We decide no-shows ourselves from the video provider's webhooks: if the student or the tutor didn't join within 10 minutes, it's a no-show. If the tutor no-shows, the student isn't charged and the tutor earns nothing. If the student no-shows, they pay in full. The Scheduling team owns the cancellation rules. Cancelling is free up to 24 hours before the lesson; after that the fee is 50% of the lesson price. Rescheduling is limited to 2 times per lesson. After a no-show or a late cancellation, the money side builds the charge just like after a normal lesson.

5. These are two separate teams. Billing builds the final charge after the lesson ends (or after a no-show or late cancellation). It charges the pre-authorized card, deducts the commission and credits the tutor's earnings. Their rules: charge within 1 hour, retry the card twice, then mark the lesson "unpaid" and email the student. Payouts runs on its own clock. Every Monday it pays out the tutors' accumulated earnings through Stripe Connect, with a 20 EUR minimum, and tutors in some countries need tax forms first. A failed payout is simply retried the next Monday. Refunds are decided by Complaints and carried out by Billing.

6. A dedicated Complaints team handles these, not general support. Students or tutors can file a complaint at any time. The outcome is one of: rejected, a refund, the complaint upheld against the tutor (and then Trust & Safety suspends them under the rule above), or both a refund and upheld. Reviews belong to a small Community team. Students rate a lesson 1-5 with a comment, up to 14 days afterwards, and a tutor's rating is the average of their last 50 reviews. Search lets you filter by rating, and the rating also decides the commission tier.

7. Trust & Safety only does onboarding and approval. After approval the tutor publishes their own profile and sets their own rate, and can change the rate any time. Search just finds tutors by subject, price, rating and free slots. It has no business rules and only shows what others publish. [improvised on ownership: no onboarding team is involved in search]

## Facilitator -> Expert (message 2)
Thanks, that clears up a lot. A few last questions before I draw the map:

1. Who owns the "Availability Slots Published" sticky: is it part of the tutor's profile, or does the Scheduling team own slots, since booking takes a slot and cancellation frees it?
2. Video room, Lesson Started/Ended and No-show Recorded: does the Scheduling team own these too, or is a different team behind the video lesson itself?
3. "Student Registered" and "Payment Card Added": who owns student accounts and the saved card? Is it a separate accounts team, or does Billing keep the card?
4. For the 10% commission tier, the "at least 100 lessons" figure: is that counted from lessons that took place, or from lessons that were paid?
5. Who sends "Lesson Reminder Sent"? Is it just Scheduling sending notifications, or a shared notifications service?

## Expert -> Facilitator (reply 2)
1. Scheduling owns the slots. The tutor enters their free hours, but after that Scheduling manages them: a booking takes a slot and a cancellation or reschedule frees it. Search only shows which slots are free. [improvised]

2. The video room itself belongs to a third-party provider. It opens the room 5 minutes before the lesson and sends us Lesson Started and Ended as webhooks. Their webhook model is messy and they've changed it twice this year. We decide no-shows ourselves from those webhooks (someone didn't join within 10 minutes), and the Scheduling team does that as part of running the lesson. [improvised on which team]

3. A student account is just a basic sign-up; no team has special rules around it. [improvised] The card is saved at Stripe, and Billing uses it for the pre-authorization and the final charge. [improvised]

4. It counts lessons that actually took place (ended), not lessons that were paid. [improvised]

5. Scheduling sends its own lesson reminders through our email/push provider. There's no shared notifications module; after the last "common" module, nobody wants one. [improvised]

## Assistant -> User (final message)
See final_message.md (verbatim copy of the last message to the user).
