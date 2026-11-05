# Bounded Context Canvas: Pricing

<!-- Pre-filled by discover-bounded-contexts (2026-11-05) via bounded-context-scaffolder, canvases only: no plan and no code yet. Source: docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml. Items marked (to re-confirm) were answered on the spot by the expert. -->

## Name
Pricing

## Purpose
Answers: **what does this lesson cost, including the platform commission?** It is one calculation for the quote, the late-cancellation fee base and the final charge. Business/finance people set the commission rules and change them every few months, separately from anything about booking.

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
- Specification/Draft Model (proposed): produces the quote that the booking locks in.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Scheduling | Query | Quote Lesson (hourly rate valid at booking, duration, tutor's completed-lesson count) | Count is carried in the request, so Pricing doesn't depend on Scheduling |
| Reviews | Event/Query | Tutor Rating Updated | For the commission tier |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Tutor rating | Query | Reviews | yes |
| Lesson Price Quoted (price, tutor share, commission) | Query response | Scheduling | n/a |

## Ubiquitous Language
- **Quote**: hourly rate x duration + commission.
- **Commission**: the platform's cut; 15% by default.
- **Commission tier**: 10% for tutors with a rating of 4.8 or more and at least 100 completed lessons.
- **Completed lesson**: a lesson that ended without a no-show.
- **Tutor share**: the part of the price that goes to the tutor.

## Business Decisions
### Standard commission
- **Given** a tutor with hourly rate 40 EUR, rating 4.5 and 30 completed lessons, **When** a 1-hour lesson is quoted, **Then** Lesson Price Quoted: 46 EUR (tutor share 40 EUR, commission 6 EUR). Check: is the commission 15% added on top of the rate, or deducted from the price? The expert said "plus the platform commission".
### Reduced commission tier
- **Given** a tutor with hourly rate 40 EUR, rating 4.8 and 100 completed lessons, **When** a 1-hour lesson is quoted, **Then** Lesson Price Quoted: 44 EUR (tutor share 40 EUR, commission 4 EUR).
### Tier boundary (needs confirmation)
- Rating 4.8 with 99 completed lessons gets 15%. Are both thresholds inclusive ("4.8 or more", "at least 100")?

## Assumptions
- The quote is locked at booking; a tier change before the lesson doesn't alter it (to re-confirm).

## Verification Metrics
- Commission changes ship without touching Scheduling or Billing.

## Open Questions
- Are the finance people who set the commission rules the same team as Billing?
