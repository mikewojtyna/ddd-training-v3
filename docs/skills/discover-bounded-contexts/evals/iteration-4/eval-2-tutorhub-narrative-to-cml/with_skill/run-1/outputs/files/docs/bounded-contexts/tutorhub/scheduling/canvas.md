# Bounded Context Canvas: Scheduling

<!-- Pre-filled by discover-bounded-contexts (2026-11-05) via bounded-context-scaffolder, canvases only: no plan and no code yet. Source: docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml. Items marked (to re-confirm) were answered on the spot by the expert. -->

## Name
Scheduling

## Purpose
Answers: **what is the state of each lesson, from booking to held or no-show?** It lets an approved tutor publish what they offer (profile, hourly rate, availability slots) and lets students find and book them. It runs the appointment until the lesson is held or a no-show is recorded, under the booking team's own cancellation and reschedule rules. The live lesson is part of it, shielded from the video provider's changing webhooks by an ACL.

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
- Execution Model (proposed): performs and tracks the lesson appointment.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor | Command | Publish profile / Set hourly rate / Publish availability slots | Rate valid at booking time applies |
| Student | Query | Search tutors (subject, price, rating, free slots) | No ranking rules |
| Student | Command | Request lesson / Reschedule / Cancel | |
| TrustAndSafety | Event | Tutor Approved / Tutor Suspended / Tutor Reinstated (proposed) | Eligibility for search and new bookings |
| Reviews | Query | Tutor rating | For search |
| Pricing | Query (response) | Lesson Price Quoted | Locked at booking (to re-confirm) |
| VideoProvider | Event | Room opened / participant joined / lesson started / ended (webhooks) | Through ACL; model changed twice this year |
| Billing | Command | Cancel Booking (reason: card hold failed) | Cancelled under Scheduling's rules (to re-confirm) |
| Complaints | Query | Lesson record (snapshot at filing) | Read-only |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Quote Lesson (rate, duration, completed-lesson count) | Query | Pricing | yes |
| Report Lesson Outcome (lesson, student, tutor, date, outcome) | Command | Reviews | yes (Reviews' outcome vocabulary) |
| Lesson Booked (locked quote with tutor share and commission) | Event | Billing | n/a for events |
| Lesson Cancelled / Late Cancellation Fee Incurred | Event | Billing | n/a for events |
| Lesson Completed / No-show Recorded (who) | Event | Billing | n/a for events |
| Lesson Reminder Sent | Event | (notification) | |

## Ubiquitous Language
- **Tutor offer**: the profile, hourly rate and availability slots a tutor maintains.
- **Slot**: a time a tutor offers for lessons.
- **Lesson request**: a student asking for a slot with a tutor.
- **Booking**: the commitment (pivotal event Lesson Booked): before it the student is shopping, after it there's an appointment.
- **Locked quote**: the price quoted at booking, which doesn't change afterwards (to re-confirm).
- **Reschedule**: moving a booked lesson; at most 2 per lesson.
- **Late cancellation**: a cancellation less than 24h before the lesson; incurs a fee.
- **Held lesson / completed lesson**: both joined; it counts even with dropped connections or a late join (to re-confirm). A lesson that ended without a no-show.
- **No-show**: someone didn't join within 10 minutes; recorded with who it was.

## Business Decisions
### Free cancellation
- **Given** a lesson booked for 2026-11-10 15:00, **When** the student cancels on 2026-11-09 14:00 (25h before), **Then** Lesson Cancelled is recorded with no fee.
### Late cancellation fee
- **Given** a lesson booked for 2026-11-10 15:00 with a locked price of 40 EUR, **When** the student cancels on 2026-11-10 09:00 (6h before), **Then** Lesson Cancelled and Late Cancellation Fee Incurred of 20 EUR (50%) are recorded.
### At most 2 reschedules
- **Given** a lesson already rescheduled twice, **When** the student asks to reschedule again, **Then** the reschedule is rejected.
### No-show
- **Given** a lesson starting at 15:00 where the tutor joined at 14:58, **When** the student hasn't joined by 15:10, **Then** No-show Recorded (student) is recorded.
### Suspended tutor
- **Given** a suspended tutor, **When** a student requests a lesson with them, **Then** the request is rejected, and the tutor doesn't appear in search.
### Rules needing GWT or confirmation
- Does the 24h/50% rule also apply when the tutor cancels late?
- No double booking of a slot (to re-confirm).
- Cancellation after a failed card hold: what the student sees and whether any fee applies.

## Assumptions
- The live lesson has no rules other than the 10-minute no-show rule (to re-confirm); if live-lesson rules appear that change on their own schedule, revisit the split from Scheduling.
- The same team runs booking and the live lesson today (to re-confirm).

## Verification Metrics
- Video-provider webhook changes never require changes outside the VideoProvider ACL.

## Open Questions
- Does the tutor accept a lesson request, or does the request become a booking straight away?
- Is Lesson Ended still emitted when nobody joined?
