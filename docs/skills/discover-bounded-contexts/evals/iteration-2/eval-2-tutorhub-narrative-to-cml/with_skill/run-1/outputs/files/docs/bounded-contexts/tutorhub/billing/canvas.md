# Bounded Context Canvas: Billing

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (bounded-context-scaffolder, canvas only, no plan or code) from the TutorHub discovery session on 2026-11-05:
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Please review before any plan or code is generated. System package for later scaffolding: tutorhub (new system).
-->

## Name
Billing

<!-- Matches `BoundedContext Billing` in the CML. -->

## Purpose
Answers: **which charge applies to how this lesson ended, and has it been collected (or reversed)?** Billing guarantees payment at booking with a card pre-authorisation, charges the student after the appointment ends, chases failed charges, splits the money into commission and the tutor's lesson earnings, and executes refunds decided by Complaints.

## Strategic Classification
- Domain: not decided (open question)
- Business model: revenue
- Evolution: custom built (on top of Stripe)

## Domain Roles
- Execution Model: performs charges and refunds.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student (app) | Command | Add payment card | Card is Billing's (student accounts are a generic component) |
| Scheduling | Event | Lesson Booked (with frozen price), Booking Expired, Lesson Ended, No-show Recorded (student/tutor), Lesson Cancelled (on time/late/by platform), Lesson Failed Technically | `LessonOutcomes`, our ACL |
| Pricing | (Published Language) | Lesson price breakdown | `FrozenLessonPrice`, inside Lesson Booked |
| Complaints | Command | Refund lesson L, amount A | `Refunds` OHS we offer |
| Complaints | Query | What was charged for lesson L? | `Refunds` OHS we offer |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Pre-authorise / charge / release / reverse | Command | Stripe (`CardPayments`) | No: Conformist, Stripe's API is an industry standard (expert) |
| Payment guaranteed / Payment guarantee failed (booking B) | Command | Scheduling (`PaymentGuarantee` OHS) | yes |
| Lesson Earnings Determined, Refund Issued (tutor's share) | Event | Payouts (`LessonEarnings`) | n/a for events |
| Unpaid-lesson email | Command | (email mechanism) | n/a |

## Ubiquitous Language
- **Pre-authorisation**: Stripe holding the lesson price on the student's card right after booking.
- **Charge**: taking the money after the appointment ends: the lesson price, the late-cancellation fee, or nothing.
- **Unpaid lesson**: a charge that failed after two retries.
- **Commission deducted**: the platform's share taken from the charge.
- **Lesson earnings**: the tutor's share of one lesson (handed to Payouts, which owns the balance).
- **Refund**: reversal of a charge decided by Complaints; the commission is reversed too.

## Business Decisions
### Charge after a lesson that took place
- **Given** a confirmed booking priced at 46.00 EUR (commission 6.00 EUR)
- **When** Lesson Ended is received
- **Then** the student is charged 46.00 EUR within 1 hour, commission 6.00 EUR is deducted and Lesson Earnings Determined (40.00 EUR) is recorded

### Student no-show pays in full
- **Given** the same booking
- **When** No-show Recorded (student) is received
- **Then** the student is charged 46.00 EUR

### Tutor no-show is free
- **Given** the same booking
- **When** No-show Recorded (tutor) is received
- **Then** nothing is charged and the pre-authorisation is released

### Late cancellation fee is charged
- **Given** the same booking
- **When** Lesson Cancelled (late) is received
- **Then** the student is charged 23.00 EUR

### Free cancellation releases the hold
- **Given** the same booking
- **When** Lesson Cancelled (on time) or (by platform) is received
- **Then** nothing is charged and the pre-authorisation is released

### Two retries, then unpaid
- **Given** a charge that failed
- **When** it fails on the second retry as well
- **Then** Lesson Marked Unpaid is recorded and the student is emailed (no booking block)

### Refund reverses commission
- **Given** a lesson charged 46.00 EUR (commission 6.00, tutor 40.00)
- **When** Complaints orders "refund lesson L, 46.00 EUR"
- **Then** Refund Issued records 46.00 EUR back to the student, 6.00 EUR commission reversed and 40.00 EUR taken from the tutor's share

## Assumptions
- Student contact details come from the generic account component.

## Verification Metrics
- Every ended appointment has exactly one charge decision within 1 hour.

## Open Questions
- How are partial refunds split between commission and tutor share (pro rata?)
- Can a refund fail, and does Complaints need to know?
- Is the pre-authorisation renewed if a lesson is rescheduled far ahead?
- Late-cancellation fee: how is it split between commission and tutor?
