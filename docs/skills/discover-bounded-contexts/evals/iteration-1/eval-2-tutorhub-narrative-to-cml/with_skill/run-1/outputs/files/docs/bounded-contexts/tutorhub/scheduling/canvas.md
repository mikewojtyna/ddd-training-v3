# Bounded Context Canvas: Scheduling

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (discover-bounded-contexts -> bounded-context-scaffolder, canvas only) on 2026-11-05 from
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Status: draft for review by the domain expert. No plan and no code yet.
Rules marked "(needs Given/When/Then)" are in the expert's words and not precise enough to test yet.
-->

## Name
Scheduling

<!-- Matches `BoundedContext Scheduling` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Turns tutors' free slots into confirmed lessons at a firm price, and owns the appointment rules: no double booking, reminders, reschedules, the cancellation policy with its late fee, and cancelling lessons whose payment isn't guaranteed or whose tutor is suspended.

The one question this context answers: **Which lesson happens when, between whom?**

## Strategic Classification
- Domain: supporting ("just has to work")
- Business model: revenue
- Evolution: custom built

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Execution Model: performs and tracks the booking and its life cycle

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor (app) | Command | Publish availability slots | publishing a slot is the tutor's acceptance |
| Student (app) | Command | Book lesson / Reschedule / Cancel |  |
| Pricing | Query | Quote for tutor T and lesson length | LessonQuotation; no rate -> no quote -> not bookable |
| TutorTrustAndSafety | Event | Tutor Approved / Suspended / Reinstated | TutorStanding |
| StudentAccounts | Event | Student Blocked / Unblocked | StudentStanding |
| Billing | Command | Booking payment guarantee failed / confirmed (booking B) | Billing speaks our terms (BookedLessons) |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Lesson Booked (quote reference) | Event | Billing, LessonDelivery | n/a |
| Lesson Rescheduled | Event | LessonDelivery | n/a |
| Lesson Cancelled (free, with reason) / Lesson Cancelled Late (50%) | Event | Billing, LessonDelivery | n/a |
| Lesson details | Query | Complaints (LessonDetails) | n/a |
| Lesson Reminder Sent | Event | (notification mechanism) | n/a |

## Ubiquitous Language
- **Availability slot**: a time the tutor offers; becomes an appointment when booked
- **Booking / Lesson Booked**: the commitment: an appointment between a student and a tutor at the quoted price
- **Quote**: Pricing's firm price for this tutor and lesson length, fixed at booking
- **Reschedule**: moving a booked lesson; keeps the original quote and pre-authorization
- **Late cancellation**: a student cancellation less than 24h before the start; 50% of the quoted price is chargeable
- **Flagged booking**: a booking whose payment guarantee failed; cancelled free if not fixed in time

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### No double booking
- **Given** Given tutor T's slot 2026-11-10 10:00-11:00 already booked by student A
- **When** student B books the same slot
- **Then** the booking is rejected

### Free cancellation
- **Given** Given a lesson starting 2026-11-10 10:00
- **When** the student cancels at 2026-11-09 09:00 (25h before)
- **Then** Lesson Cancelled (free) is recorded; nothing is chargeable

### Late cancellation fee
- **Given** Given a lesson starting 2026-11-10 10:00 quoted at 40 EUR
- **When** the student cancels at 2026-11-09 20:00 (14h before)
- **Then** Lesson Cancelled Late is recorded with a chargeable amount of 20 EUR (50%)

### Reschedule limit
- **Given** Given a lesson already rescheduled 2 times
- **When** the student reschedules it again
- **Then** the reschedule is rejected

### No booking without a rate
- **Given** Given tutor T without an hourly rate
- **When** a student books T's slot
- **Then** the booking is rejected (no quote)

### Blocked students and suspended tutors
(needs Given/When/Then) bookings are refused for a student blocked by StudentAccounts or a tutor suspended by TutorTrustAndSafety (based on their facts).

### Unguaranteed payment
(needs Given/When/Then) a flagged booking is cancelled free of charge if not fixed within 24 hours or before the lesson, whichever comes first (needs concrete times).

### Suspended tutor's lessons
(needs Given/When/Then) when a tutor is suspended, their future lessons are cancelled with no fee for the students.

## Assumptions
- A reschedule keeps the original quote and pre-authorization (confirmed by the expert, round 7).
- Lesson Requested is folded into Lesson Booked: booking is instant if the slot is free.

## Verification Metrics
- Zero double bookings.
- Share of late cancellations.

## Open Questions
- Can the tutor cancel or reschedule, and with what fee or limit?
- Reminder timing (how long before the lesson)?
