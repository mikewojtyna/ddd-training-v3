# Bounded Context Canvas: Pricing

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (bounded-context-scaffolder, canvas only, no plan or code) from the TutorHub discovery session on 2026-11-05:
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Please review before any plan or code is generated. System package for later scaffolding: tutorhub (new system).
-->

## Name
Pricing

<!-- Matches `BoundedContext Pricing` in the CML. -->

## Purpose
Answers: **what does this lesson cost, and what is the platform's commission?** One calculation gives the quote, the late-cancellation fee and the final charge, so students, tutors and Finance always see the same numbers. Finance changes the commission rules every few months, independently of booking.

## Strategic Classification
- Domain: not decided (open question)
- Business model: revenue
- Evolution: custom built (assumption)

## Domain Roles
- Specification/Draft Model: produces the priced lesson (quote) that Billing later executes.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Scheduling | Query | Quote lesson (tutor, duration) | `LessonQuoting` OHS we offer; price frozen at booking |
| Scheduling | Command | Record lesson took place (tutor) | Counts towards the 100-lesson tier |
| Tutor Catalog | Command | Register tutor hourly rate | `TutorRateRegistration` OHS |
| Reviews | Command | Register tutor rating | `TutorRatingRegistration` OHS |
| Finance | Command | Change commission rules | Every few months |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Lesson price breakdown (price, commission, late-cancellation fee), in our Published Language | Event payload | Billing (`FrozenLessonPrice`, carried inside Scheduling's Lesson Booked) | n/a |

## Ubiquitous Language
- **Hourly rate**: the tutor's own price per hour, in EUR.
- **Lesson price**: hourly rate x duration + commission.
- **Commission**: the platform's share: 15%, or 10% for tutors in the reduced tier.
- **Reduced commission tier**: tutor rating of 4.8 or more and at least 100 lessons that took place.
- **Lesson that took place**: both student and tutor joined (no-shows and late cancellations don't count).
- **Late-cancellation fee**: 50% of the lesson price.
- **Quote**: the price shown before booking; frozen at booking and charged as-is.

## Business Decisions
### Standard commission
- **Given** a tutor with hourly rate 40 EUR, rating 4.5 and 30 lessons that took place
- **When** a 1-hour lesson is quoted
- **Then** the lesson price is 40 + 15% = 46.00 EUR (commission 6.00 EUR)

### Reduced commission tier
- **Given** a tutor with hourly rate 40 EUR, rating 4.8 and 100 lessons that took place
- **When** a 1-hour lesson is quoted
- **Then** the lesson price is 40 + 10% = 44.00 EUR (commission 4.00 EUR)

### Tier needs both conditions
- **Given** a tutor with rating 4.9 and 99 lessons that took place
- **When** a lesson is quoted
- **Then** the commission is 15%

### Late-cancellation fee
- **Given** a booked lesson priced at 46.00 EUR
- **When** the late-cancellation fee is calculated
- **Then** the fee is 23.00 EUR

<!-- Needs confirmation: is the commission added on top of the rate (student pays rate + commission), as the expert's formula reads, while Billing "deducts commission" from what it collects? Worked examples above follow "rate x duration + commission". -->

## Assumptions
- The whole price, including the tier, is frozen at booking (confirmed).

## Verification Metrics
- The quote, the final charge and the late fee never disagree for the same booking.

## Open Questions
- Is the commission a percentage of the tutor's rate (added on top) or of the total the student pays?
- Does the late-cancellation fee also carry commission, and how is it split?
- Are durations other than whole hours allowed?
