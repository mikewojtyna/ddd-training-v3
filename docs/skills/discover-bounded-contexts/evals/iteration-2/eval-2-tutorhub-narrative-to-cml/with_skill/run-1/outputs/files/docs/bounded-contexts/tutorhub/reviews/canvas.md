# Bounded Context Canvas: Reviews

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (bounded-context-scaffolder, canvas only, no plan or code) from the TutorHub discovery session on 2026-11-05:
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Please review before any plan or code is generated. System package for later scaffolding: tutorhub (new system).
-->

## Name
Reviews

<!-- Matches `BoundedContext Reviews` in the CML. -->

## Purpose
Answers: **what do students think of this tutor?** The Community team collects a 1-5 review with a comment after each lesson and keeps each tutor's rating, which students use to choose tutors and Finance uses for the commission tier.

## Strategic Classification
- Domain: supporting (assumption)
- Business model: engagement
- Evolution: custom built (assumption)

## Domain Roles
- Engagement Context: ratings help students choose and bring them back.
- Analysis/Audit Model: rates the quality of delivered lessons.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Scheduling | Event | Lesson Ended (took place) | `LessonTookPlace`, opens the review window |
| Student (app) | Command | Submit review (1-5, comment) | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Register tutor rating | Command | Pricing (`TutorRatingRegistration` OHS) | yes |
| Tutor Rating Updated | Event | Tutor Catalog (`TutorRating`) | n/a for events |
| Review Requested | Event | (notification mechanism) | n/a |

## Ubiquitous Language
- **Review**: a student's 1-5 score with a comment for one lesson.
- **Review window**: 14 days after the lesson.
- **Rating**: the average of the tutor's last 50 reviews.

## Business Decisions
### Review within 14 days
- **Given** a lesson that ended on 1 March
- **When** the student submits a review on 16 March
- **Then** the review is rejected (window closed)

### Score range
- **Given** a lesson that took place
- **When** the student submits a score of 6
- **Then** the review is rejected

### Rating is the average of the last 50 reviews
- **Given** a tutor whose last 50 reviews average 4.82 and a 51st-newest review of 1
- **When** a new review of 5 is submitted
- **Then** the rating is the average of the 50 newest reviews (the oldest one drops out) and Tutor Rating Updated is published

<!-- The last example needs concrete numbers from the team before it becomes a test. -->

## Assumptions
- Only lessons that took place open a review (not no-shows).

## Verification Metrics
- Share of lessons that get a review within 14 days.

## Open Questions
- Can tutors review students?
- Can reviews be moderated or removed (e.g. after a complaint), and by whom?
- Is a rating shown before a tutor has any reviews?
