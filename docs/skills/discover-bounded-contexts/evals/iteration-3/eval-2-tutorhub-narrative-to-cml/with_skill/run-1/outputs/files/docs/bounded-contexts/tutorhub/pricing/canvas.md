# Bounded Context Canvas: Pricing

<!--
Pre-filled by discover-bounded-contexts on 2026-11-05 (report: docs/context-discovery/tutorhub/2026-11-05-event-storming/discovery.md;
map: src/main/cml/TutorHub-Context-Map.cml). Based on the ddd-crew Bounded Context Canvas v5.
Status: draft for review. No plan or code yet. bounded-context-scaffolder builds code only from rules written as strict Given/When/Then.
Rule references (R<round>-<answer>) point to the Q&A in the discovery report.
-->

## Name
Pricing

## Purpose
Answers: **What does a lesson cost, and what is our commission?** Calculates the lesson price and the platform commission with finance's rules, so the quote, the late fee base and the final charge all come from one calculation (R1-3).

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md; suggested by discovery, to confirm -->
- Specification/Draft Model (suggested): produces the quote that the booking fixes.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Scheduling | Query | Quote request (tutor rate, duration, held-lesson count) | Pricing [U,OHS,PL]->[D,ACL] Scheduling : LessonPriceQuote |
| Reviews | Event | Tutor Rating Updated | Reviews [U,S,PL]->[D,C,ACL] Pricing : TutorRating |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Lesson quote (price, commission) | Query result | Scheduling | n/a (we are upstream) |

## Ubiquitous Language
- **Lesson price**: hourly rate × duration + platform commission
- **Commission**: the platform's share: 15% by default
- **Commission tier**: 10% for tutors with a rating of at least 4.8 and at least 100 held lessons; worked out fresh for each quote

## Business Decisions
<!-- In the expert's words from discovery. None is a strict Given/When/Then yet: each needs a concrete example before bounded-context-scaffolder can delegate it. -->
### Price formula
- Expert: "hourly rate × duration + platform commission" (R1-3)
- Status: **needs Given/When/Then** (concrete; write GWT with numbers)

### Commission tier
- Expert: "Commission is 15%, or 10% for tutors with a rating of at least 4.8 and at least 100 lessons." (R1-3)
- Status: **needs Given/When/Then** (concrete; write GWT incl. boundary 4.8 / 100)

### Tier as of quote
- Expert: "Pricing works out the commission tier fresh each time and doesn't keep it" (R6-2); the quote booked freezes it (R8-1)
- Status: **needs Given/When/Then** (write GWT)

## Assumptions
- Pricing keeps the latest rating per tutor from Reviews events (it "reads" the rating, R6-2).

## Verification Metrics
- Finance can change commission rules without touching Scheduling or Billing.

## Open Questions
- Is the commission added on top of the tutor's rate for the student, or taken out of it? The formula reads "on top"; "Platform Commission Deducted" reads "out of".
- Rounding and currency (EUR only, R4-2).
- Strategic classification.
