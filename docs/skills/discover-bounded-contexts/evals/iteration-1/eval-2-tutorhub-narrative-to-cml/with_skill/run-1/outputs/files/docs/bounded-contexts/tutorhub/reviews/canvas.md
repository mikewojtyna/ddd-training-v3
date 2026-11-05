# Bounded Context Canvas: Reviews

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
Reviews

<!-- Matches `BoundedContext Reviews` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Collects honest reviews after lessons and turns them into each tutor's rating, which students rely on when choosing a tutor and which feeds the commission tier. Honest ratings are part of TutorHub's edge.

The one question this context answers: **How good is this tutor in students' eyes?**

## Strategic Classification
- Domain: core (with TutorTrustAndSafety)
- Business model: engagement
- Evolution: custom built

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Engagement Context: honest ratings keep students choosing TutorHub
- Analysis/Audit Model: rates the outcome of lessons

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| LessonDelivery | Event | Lesson Ended | DeliveredLessons -> Review Requested |
| Student (app) | Command | Submit review | 1-5 stars + comment |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Review Requested | Event | (student notification) | n/a |
| Tutor Rating Updated | Event | Pricing (TutorRating), search read model | n/a |

## Ubiquitous Language
- **Review**: 1 to 5 stars with a comment from the student about one lesson
- **Review window**: 14 days after the lesson
- **Rating**: the average of the tutor's last 50 reviews

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Review window
- **Given** Given lesson L ended on 2026-11-01
- **When** the student submits a review on 2026-11-16 (15 days later)
- **Then** the review is rejected

### Rating = average of last 50
- **Given** Given tutor T with 50 reviews averaging 4.70, the oldest being 3 stars
- **When** a new 5-star review is submitted
- **Then** T's rating becomes 4.74 (oldest review drops out)

## Assumptions
- The Community team owns reviews (expert round 1).
- Reviews are requested only after a delivered lesson.

## Verification Metrics
- Share of delivered lessons that get a review within 14 days.

## Open Questions
- May a student review a tutor who no-showed?
- Can a review be removed or hidden (moderation), and who decides?
- Consider renaming "Tutor Rating Updated" to "Tutor Rating Recalculated".
