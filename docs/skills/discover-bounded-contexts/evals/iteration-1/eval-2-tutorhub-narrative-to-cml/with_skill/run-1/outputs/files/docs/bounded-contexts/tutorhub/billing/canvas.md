# Bounded Context Canvas: Billing

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
Billing

<!-- Matches `BoundedContext Billing` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Makes sure students pay what they owe for each lesson: holds money at booking, charges after the lesson or a late cancellation, retries and marks unpaid lessons, re-charges when the card is fixed, deducts the commission, records the tutor's earnings for Payouts, and executes refunds Complaints decides.

The one question this context answers: **What does the student owe for this lesson, and is it paid?**

## Strategic Classification
- Domain: supporting ("just has to work")
- Business model: revenue
- Evolution: custom built (on Stripe)

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Execution Model: performs charges, retries and refunds

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Scheduling | Event | Lesson Booked (quote reference) | BookedLessons -> pre-authorize |
| Scheduling | Event | Lesson Cancelled Late (chargeable 50%) / Lesson Cancelled (free) | charge / release |
| LessonDelivery | Event | Lesson Ended / Student No-show / Tutor No-show | LessonOutcomes |
| Pricing | Query | Quoted price; commission split of amount A | CommissionSplit |
| StudentAccounts | Event | Payment Card Added / Updated | StudentPaymentStanding -> retry pre-auth / re-charge |
| Complaints | Command | Refund lesson L, amount A | Refunds; Complaints speaks our terms |
| Stripe | Query/Command | pre-auth, charge, refund | Conformist (CardPayments): industry standard |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Booking payment guarantee failed / confirmed | Command | Scheduling | yes (in Scheduling's terms) |
| Report lesson unpaid / outstanding payment settled | Command | StudentAccounts | yes (in Student Accounts' terms) |
| Lesson Earnings Recorded / Lesson Earnings Reversed | Event | Payouts | n/a |
| Refund Issued | Event | Complaints | n/a |

## Ubiquitous Language
- **Pre-authorization**: Stripe holding the quoted amount right after booking; the booking doesn't wait for it
- **Charge**: taking the money: full quoted price after the lesson or a student no-show, nothing after a tutor no-show, the late-cancellation amount after a late cancellation
- **Unpaid lesson**: a charge that failed after 2 card retries
- **Outstanding payment settled**: an unpaid lesson charged successfully after the student updated the card
- **Lesson earnings**: the tutor's share of a charged amount, recorded per lesson for Payouts
- **Refund**: returning an amount decided by Complaints; reverses the tutor's share and our commission

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Charge after the lesson
- **Given** Given lesson L quoted at 40 EUR
- **When** Lesson Ended is recorded
- **Then** the student is charged 40 EUR within 1 hour

### No charge on tutor no-show
- **Given** Given lesson L quoted at 40 EUR
- **When** Tutor No-show Recorded
- **Then** the student is not charged and the pre-authorization is released

### Full charge on student no-show
- **Given** Given lesson L quoted at 40 EUR
- **When** Student No-show Recorded
- **Then** the student is charged 40 EUR

### Retries then unpaid
- **Given** Given a charge of 40 EUR that failed
- **When** the 2 card retries also fail
- **Then** Lesson Marked Unpaid is reported to StudentAccounts and the student is emailed

### Pre-auth failure
- **Given** Given lesson L just booked
- **When** the pre-authorization fails
- **Then** Billing reports "payment guarantee failed for booking L" to Scheduling and asks the student to update the card

### Refund reversal
- **Given** Given lesson L charged 40 EUR with 6 EUR commission and 34 EUR tutor share
- **When** Complaints asks to refund 40 EUR
- **Then** Refund Issued and Lesson Earnings Reversed (34 EUR) are recorded and the 6 EUR commission is returned (values assume commission is deducted from the price, see Pricing's open question; partial refunds are split by Pricing, needs values)

## Assumptions
- No collection process exists beyond card update and re-charge: no agencies, no write-off rules (expert round 4).

## Verification Metrics
- Share of lessons charged within 1 hour.
- Unpaid lessons settled within N days.

## Open Questions
- May a refund exceed the amount charged (cap)?
- Timing of retries (how far apart)?
