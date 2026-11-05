# Bounded Context Canvas: Scheduling

<!--
Pre-filled by discover-bounded-contexts on 2026-11-05 (report: docs/context-discovery/tutorhub/2026-11-05-event-storming/discovery.md;
map: src/main/cml/TutorHub-Context-Map.cml). Based on the ddd-crew Bounded Context Canvas v5.
Status: draft for review. No plan or code yet. bounded-context-scaffolder builds code only from rules written as strict Given/When/Then.
Rule references (R<round>-<answer>) point to the Q&A in the discovery report.
-->

## Name
Scheduling

## Purpose
Answers: **When can this tutor be booked, and what happened to each booked lesson?** Turns an approved tutor's offer (profile, EUR rate, availability slots) into booked lessons and knows what happened to each one: booking, rescheduling, cancelling, reminders, running the lesson with the video provider and recording no-shows. Single owner of "is this slot bookable right now?" (R4-1).

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md; suggested by discovery, to confirm -->
- Execution Model: performs and tracks the lesson appointment from booking to its outcome.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student (app) | Command | Request a slot; book; reschedule; cancel |  |
| Tutor (app) | Command | Edit profile, EUR hourly rate, unbooked availability slots; cancel a booked lesson |  |
| TrustAndSafety | Event | Tutor Approved / Tutor Suspended (reason) / Tutor Reinstated | TrustAndSafety [U,S,PL]->[D,C,ACL] Scheduling : TutorEligibility |
| Billing | Event | Lesson Unpaid (lesson L, student S) / Lesson Paid | Billing [U,S,PL]->[D,C,ACL] Scheduling : StudentPaymentStanding |
| VideoProvider | Event | Room / participant joined / started / ended webhooks | VideoProvider [U]->[D,ACL] Scheduling : VideoRoomWebhooks; the provider's model is messy and changed twice this year (R2-1, R6-5) |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Quote request (rate, duration, held-lesson count) | Query | Pricing | yes: Pricing [U,OHS,PL]->[D,ACL] Scheduling : LessonPriceQuote |
| Room set-up for a booked lesson | Command | VideoProvider | yes (ACL) |
| Lesson Booked (with booked quote: price, commission) | Event | Billing | n/a for events |
| Lesson Held / Student No-show / Tutor No-show | Event | Billing; Lesson Held also Reviews | n/a for events |
| Late Cancellation Fee Due (50% of booked price) | Event | Billing | n/a for events |
| Lesson Cancelled (by student, tutor or platform) | Event | Billing | n/a for events |
| Lesson details | Event | Complaints | n/a for events |

## Ubiquitous Language
- **Offer**: an approved tutor's profile, EUR hourly rate and availability slots
- **Slot**: a time the tutor offers; free until booked
- **Booking**: the student takes a free slot; no tutor acceptance; the booking is the truth for the slot
- **Booked quote**: the price and commission quoted by Pricing, fixed at booking
- **Lesson held**: a lesson that actually ended; no-shows and cancellations are not held
- **No-show**: a party (student or tutor) hasn't joined within 10 minutes of the start
- **Late cancellation**: a cancellation less than 24 hours before the lesson

## Business Decisions
<!-- In the expert's words from discovery. None is a strict Given/When/Then yet: each needs a concrete example before bounded-context-scaffolder can delegate it. -->
### Booking cut-off
- Expert: "A slot can be booked up to 2 hours before it starts" (R4-1)
- Status: **needs Given/When/Then** (concrete; write GWT)

### Booking wins a race
- Expert: "If a tutor removes a free slot at the same moment a student books it, the booking wins." (R4-1)
- Status: **needs Given/When/Then** (concrete; write GWT)

### Free cancellation window
- Expert: "cancelling is free up to 24 hours before the lesson. After that, the late cancellation fee is 50% of the lesson price" (R1-2)
- Status: **needs Given/When/Then** (concrete; write GWT with a price)

### Reschedule limit
- Expert: "a lesson can be rescheduled at most twice" (R1-2)
- Status: **needs Given/When/Then** (concrete; write GWT)

### No-show
- Expert: "If the student or the tutor hasn't joined within 10 minutes, we record a no-show" (R1)
- Status: **needs Given/When/Then** (concrete; write GWT)

### Unpaid students can't book
- Expert: "A student with an unpaid lesson can't book again until it's paid." (R3-1, R7-2)
- Status: **needs Given/When/Then** (concrete; write GWT)

### Suspended tutor
- Expert: "A suspended tutor's profile is hidden from search and they can't receive new bookings. Lessons already booked with that tutor are cancelled free of charge for the student, and the student is notified" (R1-4)
- Status: **needs Given/When/Then** (write GWT)

### Booked slots are locked
- Expert: "The tutor can't delete or change a booked slot. They have to cancel the lesson, which is free for the student" (R3-2)
- Status: **needs Given/When/Then** (write GWT)

### Held-lesson count
- Expert: count of lessons that ended, sent with each quote request (R3-3, R7-1)
- Status: **needs Given/When/Then** (write GWT)

## Assumptions
- Scheduling republishes Pricing's booked quote on Lesson Booked so Billing doesn't depend on Pricing (design decision, R8-1 leaves the mechanism open).
- Profile and rate have no rules of their own (R4-2); revisit if moderation or rate limits appear.

## Verification Metrics
- No double bookings; no lesson charged differently from its booked quote.

## Open Questions
- Can a quote expire between Lesson Price Quoted and Lesson Booked?
- Does a tutor cancelling a lesson have any consequence for the tutor?
- Strategic classification.
