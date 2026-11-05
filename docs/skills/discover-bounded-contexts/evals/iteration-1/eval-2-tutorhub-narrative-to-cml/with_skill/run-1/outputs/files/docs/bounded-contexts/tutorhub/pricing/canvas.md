# Bounded Context Canvas: Pricing

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
Pricing

<!-- Matches `BoundedContext Pricing` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Calculates what a lesson costs and what TutorHub's cut is, with rules owned by business/finance that change every few months independently of booking. Gives firm quotes and splits any charged or refunded amount into commission and tutor share. Never charges.

The one question this context answers: **What does a lesson cost, and what is our cut?**

## Strategic Classification
- Domain: supporting ("matters to the business, but competitors do similar things")
- Business model: revenue
- Evolution: custom built

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Specification/Draft Model: produces the quote that the booking and the charge rely on

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor (app) | Command | Set hourly rate | rate valid at booking time applies |
| Scheduling | Query | Quote for tutor T and lesson length | LessonQuotation |
| Billing | Query | Quoted price by quote reference / commission split of amount A | CommissionSplit |
| Reviews | Event | Tutor Rating Updated | TutorRating, ACL |
| LessonDelivery | Event | Lesson Ended / Student No-show / Tutor No-show | DeliveredLessonOutcomes, ACL; Pricing applies its own counting rule |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Lesson Price Quoted | Event/Query result | Scheduling, Billing | n/a |
| Hourly Rate Set | Event | search read model | n/a |
| Commission split | Query result | Billing | n/a |

## Ubiquitous Language
- **Hourly rate**: the tutor's own price per hour; may change any time, the rate valid at booking time applies
- **Quote**: firm price = hourly rate x duration + commission, with the commission tier frozen at booking
- **Commission tier**: 15% by default; 10% for tutors with rating >= 4.8 and >= 100 delivered lessons
- **Delivered lesson**: a lesson that ended or where the student didn't show; tutor no-shows and cancellations don't count
- **Tutor share**: the amount left for the tutor after commission

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Default commission
- **Given** Given tutor T with rate 40 EUR/h, rating 4.5 and 30 delivered lessons
- **When** a 1-hour lesson is quoted
- **Then** the quote is 40 + 15% commission (needs confirmation: is commission added on top, or taken out of the 40?)

### Reduced commission tier
- **Given** Given tutor T with rating 4.8 and 100 delivered lessons
- **When** a lesson is quoted
- **Then** the commission tier is 10% and is frozen in the quote

### Counting delivered lessons
- **Given** Given tutor T with 99 delivered lessons
- **When** Student No-show Recorded for T's lesson
- **Then** T has 100 delivered lessons (a Tutor No-show or a cancellation wouldn't count)

### Rate valid at booking
- **Given** Given T's rate 40 EUR/h, changed to 50 EUR/h after lesson L was booked
- **When** L is charged
- **Then** L's price uses 40 EUR/h

### Commission on late fees
(needs Given/When/Then) commission is taken from a late-cancellation fee the same way as from a lesson; the tutor gets their share of the 50% (needs concrete values).

## Assumptions
- The 50% late-cancellation share is Scheduling's rule, not Pricing's (expert round 4).
- The indirect cycle Pricing -> Scheduling -> LessonDelivery -> Pricing is the business's own feedback loop and is kept on purpose (see the CML).

## Verification Metrics
- A change to commission tiers ships without touching Scheduling or Billing.

## Open Questions
- Is the commission added on top of rate x duration (student pays more) or deducted from it? The expert said "rate x duration + platform commission" and also "Platform Commission Deducted".
- Does a refunded lesson still count towards the 100? (Currently yes: only tutor no-shows and cancellations are excluded.)
