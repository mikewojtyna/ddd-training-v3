# Bounded Context Canvas: Scheduling

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (bounded-context-scaffolder, canvas only, no plan or code) from the TutorHub discovery session on 2026-11-05:
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Please review before any plan or code is generated. System package for later scaffolding: tutorhub (new system).
-->

## Name
Scheduling

<!-- Matches `BoundedContext Scheduling` in the CML. Lesson delivery (video room, no-show) was merged in during discovery. -->

## Purpose
Answers: **what is the state of each lesson appointment, from booking to how it ended?** It turns tutors' free slots into booked lessons without ever double-booking, enforces the cancellation and rescheduling rules, runs the lesson through the video provider and decides how each appointment ended (took place, no-show, cancelled, failed technically).

## Strategic Classification
- Domain: not decided (open question)
- Business model: revenue (assumption: every lesson sold goes through it)
- Evolution: custom built (assumption)

## Domain Roles
- Execution Model: performs and tracks each lesson appointment.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor (app) | Command | Publish / change / withdraw free slot | Not a booked slot |
| Student (app) | Command | Request lesson, Book lesson, Reschedule, Cancel | |
| Billing | Command | Payment guaranteed / Payment guarantee failed (booking B) | `PaymentGuarantee` OHS that we offer |
| Tutor Vetting | Command | Open / Close / Reopen tutor calendar | `TutorCalendar` OHS that we offer |
| Video provider | Event | Room opened, participant joined/left, lesson started/ended (webhooks) | `VideoRooms`, behind our ACL (messy model, changed twice this year) |
| Complaints | Query | Lesson record (took place? who joined, when?) | `LessonRecord` OHS |
| Tutor Catalog | Query | Free slots | `FreeSlots` OHS |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Quote lesson (tutor, duration) | Query | Pricing (`LessonQuoting` OHS) | yes |
| Record lesson took place | Command | Pricing (`LessonQuoting` OHS) | yes |
| Lesson Booked (with frozen price), Booking Confirmed/Expired, Lesson Ended, No-show Recorded (student/tutor), Lesson Cancelled (on time/late/by platform), Lesson Failed Technically | Event | Billing (`LessonOutcomes`) | n/a for events |
| Lesson Ended (took place) | Event | Reviews (`LessonTookPlace`) | n/a for events |
| Lesson Reminder Sent | Event | (notification mechanism) | n/a |

## Ubiquitous Language
- **Free slot**: a time a tutor offers; can be withdrawn unless booked.
- **Lesson request**: a student asks for a slot and sees the quote; not a commitment.
- **Booking**: the commitment; the price is frozen at this moment. **Confirmed** once payment is guaranteed; **expired** if it isn't within 1 hour.
- **Tutor calendar**: open (tutor may teach) or closed (suspended); opened/closed by Trust & Safety.
- **Reschedule**: moving a booked lesson; at most 2 per lesson, technical-failure reschedules don't count.
- **Cancellation**: **on time** (24 h or more before), **late** (less than 24 h), **by platform** (tutor suspended).
- **Took place**: both student and tutor joined.
- **No-show**: the student or the tutor didn't join within 10 minutes of the start.
- **Technical failure**: the room didn't open or the provider was down.

## Business Decisions
### No double booking, first booking wins
- **Given** tutor X's free slot Monday 10:00-11:00
- **When** student A books it and then student B tries to book it
- **Then** A's booking is recorded and B is told the slot is gone

### A booked slot can't be withdrawn
- **Given** a slot booked by student A
- **When** the tutor withdraws the slot
- **Then** the withdrawal is rejected

### Failed payment guarantee expires the booking after 1 hour
- **Given** a booking at 12:00 whose payment guarantee failed
- **When** 13:00 passes without "payment guaranteed"
- **Then** Booking Expired is recorded and the slot is free again

### Free cancellation up to 24 h
- **Given** a lesson on Friday 18:00
- **When** the student cancels on Thursday 17:00 (25 h before)
- **Then** Lesson Cancelled (on time) is recorded

### Late cancellation
- **Given** a lesson on Friday 18:00
- **When** the student cancels on Friday 09:00 (9 h before)
- **Then** Lesson Cancelled (late) is recorded

### At most two reschedules
- **Given** a lesson already rescheduled twice by the student
- **When** the student reschedules again
- **Then** the reschedule is rejected

### Technical failure gives a free reschedule
- **Given** a lesson already rescheduled twice
- **When** the video room doesn't open
- **Then** Lesson Failed Technically is recorded and the student may reschedule once more without it counting

### No-show after 10 minutes
- **Given** a lesson starting at 15:00 where the tutor joined at 14:58
- **When** the student hasn't joined by 15:10
- **Then** No-show Recorded (student) is recorded

### Closing a suspended tutor's calendar
- **Given** tutor X with lessons booked on Tuesday and Thursday
- **When** Close tutor calendar (X) is received
- **Then** both lessons are cancelled (by platform) and X can't receive new bookings

## Assumptions
- The quote shown at request time is the price frozen at booking (Pricing).

## Verification Metrics
- Zero double bookings; every booked lesson ends in exactly one outcome.

## Open Questions
- Can tutors cancel or reschedule too, and do the 24 h and two-reschedule rules apply to them?
- During the 1-hour card-fix window, is the slot already taken for other students?
- Is a reschedule less than 24 h before the lesson allowed and free?
- Is the reminder's timing a rule anyone cares about? (Expert: no rules today.)
