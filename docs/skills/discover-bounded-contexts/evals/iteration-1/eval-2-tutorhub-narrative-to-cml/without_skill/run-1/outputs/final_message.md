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
