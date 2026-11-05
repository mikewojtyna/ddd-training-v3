# Bounded Context Canvas: Billing

<!--
Pre-filled by discover-bounded-contexts on 2026-11-05 (report: docs/context-discovery/tutorhub/2026-11-05-event-storming/discovery.md;
map: src/main/cml/TutorHub-Context-Map.cml). Based on the ddd-crew Bounded Context Canvas v5.
Status: draft for review. No plan or code yet. bounded-context-scaffolder builds code only from rules written as strict Given/When/Then.
Rule references (R<round>-<answer>) point to the Q&A in the discovery report.
-->

## Name
Billing

## Purpose
Answers: **What is charged for this lesson, and is it paid?** Takes the student's money for each lesson outcome using the booked quote, chases failed charges, splits commission and tutor earnings, and carries out refunds decided by Complaints.

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md; suggested by discovery, to confirm -->
- Execution Model: performs the charge, retries and refunds.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student (app) | Command | Add payment card |  |
| Scheduling | Event | Lesson Booked (+ booked quote), Lesson Held, Student No-show, Tutor No-show, Late Cancellation Fee Due, Lesson Cancelled | Scheduling [U,S,PL]->[D,C,ACL] Billing : LessonOutcomes |
| Complaints | Event | Refund Decided ("refund lesson L, amount A") | Complaints [U,S,PL]->[D,C,ACL] Billing : RefundDecisions |
| Stripe | API | cards, pre-authorisation, charge, refund results | Stripe [U,OHS,PL]->[D,CF] Billing : CardPayments (Conformist, R6-5) |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Pre-authorise / charge / refund / release hold | Command | Stripe | no: Conformist by decision (industry-standard API, R6-5) |
| Lesson Unpaid / Lesson Paid | Event | Scheduling | n/a for events |
| Tutor Earnings Credited / Tutor Earnings Reversed | Event | Payouts | n/a for events |
| What was charged for a lesson | Event | Complaints | n/a for events |

## Ubiquitous Language
- **Pre-authorisation**: a hold on the student's card taken at booking
- **Charge**: taking the lesson price or fee from the pre-authorised card
- **Unpaid lesson**: a lesson whose charge still failed after retries
- **Tutor earnings**: the lesson charge minus the platform commission, credited per lesson
- **Earnings reversed**: refund minus commission, taken back from the tutor

## Business Decisions
<!-- In the expert's words from discovery. None is a strict Given/When/Then yet: each needs a concrete example before bounded-context-scaffolder can delegate it. -->
### Charge timing and retries
- Expert: "charge within 1 hour and retry the card twice. If it still fails, the lesson is marked 'unpaid' and the student gets an email." (R1-3)
- Status: **needs Given/When/Then** (concrete; write GWT)

### No-show charges
- Expert: "if the tutor doesn't show up, the student isn't charged. If the student doesn't show up, they pay in full." (R1-3)
- Status: **needs Given/When/Then** (concrete; write GWT)

### Charge equals booked quote
- Expert: "for a held lesson or a student no-show, the charge equals the quote the student saw. The late fee is 50% of that price" (R8-1)
- Status: **needs Given/When/Then** (write GWT)

### Refund reverses earnings
- Expert: "The refunded amount, minus our commission, is taken off their next payout" (R4-4)
- Status: **needs Given/When/Then** (write GWT with amounts)

### Unpaid lifted
- Expert: "Billing marks the lesson unpaid and is the one that lifts it once the money comes in" (R3-1)
- Status: **needs Given/When/Then** (vague: how does the money come in? needs GWT)

## Assumptions
- Billing gets price and commission from the booked quote carried on Lesson Booked (design decision; R8-1).

## Verification Metrics
- Every held lesson charged within 1 hour; unpaid rate.

## Open Questions
- When is the card pre-authorised for lessons booked far ahead, and when is the hold released if nothing is charged?
- How does a student pay off an unpaid lesson?
- Can a refund exceed what was charged?
- Strategic classification.
