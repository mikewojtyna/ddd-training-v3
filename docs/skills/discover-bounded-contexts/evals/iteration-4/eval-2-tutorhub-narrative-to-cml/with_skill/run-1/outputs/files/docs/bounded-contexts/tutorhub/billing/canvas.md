# Bounded Context Canvas: Billing

<!-- Pre-filled by discover-bounded-contexts (2026-11-05) via bounded-context-scaffolder, canvases only: no plan and no code yet. Source: docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml. Items marked (to re-confirm) were answered on the spot by the expert. -->

## Name
Billing

## Purpose
Answers: **what does the student pay for each lesson, and has it been collected?** The Billing team holds the money on the student's card after booking, builds the final charge from the lesson's outcome, collects it with its own retry rules, deducts the commission, reports what each lesson earned the tutor, and executes the refunds Complaints decides.

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
- Execution Model (proposed): performs collection of each lesson's charge.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student | Command | Add payment card | Card stored at Stripe; Billing keeps a reference |
| Scheduling | Event | Lesson Booked (locked quote with breakdown) | Triggers the card hold |
| Scheduling | Event | Lesson Cancelled / Late Cancellation Fee Incurred / Lesson Completed / No-show Recorded | Triggers the final charge |
| Complaints | Command | Refund Lesson (lesson, amount, take back tutor share yes/no) | Billing's refund service |
| Complaints | Query | Charge and fee for a lesson | Read-only |
| Stripe | Event | Hold / charge / refund results | Through ACL |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Hold / charge / refund | Command | Stripe | yes |
| Cancel Booking (reason: card hold failed) | Command | Scheduling | yes (to re-confirm) |
| Lesson Earnings Recorded (lesson, tutor, amount) | Event | Payouts | n/a for events |
| Lesson Earnings Reversed (lesson, tutor, amount) | Event | Payouts | n/a for events (to re-confirm) |
| Charge Marked Unpaid | Event | (email to student) | |

## Ubiquitous Language
- **Card hold (pre-authorisation)**: Stripe holding the lesson price on the student's card right after booking.
- **Final charge**: the locked lesson price, or the late-cancellation fee, or the full price on a student no-show, or nothing on a tutor no-show.
- **Commission deduction**: taking the platform commission from the locked quote's breakdown (no recalculation of tiers).
- **Lesson earnings**: what one lesson earned the tutor ("Tutor Earnings Credited" on the board).
- **Unpaid**: a charge that failed after the retries.
- **Refund**: reversing the student's charge as decided by Complaints.

## Business Decisions
### Student no-show pays in full
- **Given** a lesson with a locked price of 46 EUR, **When** No-show Recorded (student) arrives, **Then** the student is charged 46 EUR.
### Tutor no-show pays nothing
- **Given** a lesson with a locked price of 46 EUR, **When** No-show Recorded (tutor) arrives, **Then** nothing is charged and the hold is released.
### Charge retries
- **Given** a completed lesson whose first charge attempt fails, **When** two retries within the 1-hour charging window also fail, **Then** Charge Marked Unpaid is recorded and the student is emailed. (Retry timing needs GWT.)
### Failed card hold (to re-confirm)
- **Given** a new booking whose hold Stripe refuses, **When** the student hasn't added a working card within 1 hour, **Then** Billing asks Scheduling to cancel the booking.

## Assumptions
- Unpaid students are not blocked today.
- Does Billing record lesson earnings for a charge that ends up unpaid? Not discussed.

## Verification Metrics
- Commission tier changes in Pricing never require Billing changes.

## Open Questions
- Card holds expire (about 7 days at Stripe): who renews the hold for lessons booked further ahead?
- A refund for a lesson still marked unpaid: cancel the debt instead of reversing a charge?
