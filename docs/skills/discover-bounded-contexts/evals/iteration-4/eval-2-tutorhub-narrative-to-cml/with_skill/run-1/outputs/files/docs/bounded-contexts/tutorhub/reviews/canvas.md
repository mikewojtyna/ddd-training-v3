# Bounded Context Canvas: Reviews

<!-- Pre-filled by discover-bounded-contexts (2026-11-05) via bounded-context-scaffolder, canvases only: no plan and no code yet. Source: docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml. Items marked (to re-confirm) were answered on the spot by the expert. -->

## Name
Reviews

## Purpose
Answers: **what is this tutor's rating, based on students' lesson reviews?** The small Community team collects students' 1-5 reviews within 14 days of a held lesson and keeps each tutor's rating. Search shows the rating, and Pricing uses it for the commission tier.

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
- Analysis/Audit Model (proposed): assesses tutors from students' feedback.
- Engagement Context (proposed): ratings help students choose tutors.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Scheduling | Command | Report Lesson Outcome (lesson, student, tutor, date, outcome: held, student no-show, tutor no-show, late-cancelled) | Reviews' own outcome vocabulary; Scheduling translates into it |
| Student | Command | Submit review (1-5 + comment) | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Review Requested | Event | (student notification) | n/a |
| Tutor Rating Updated / tutor rating | Event / Query | Pricing, Scheduling (search) | n/a (upstream OHS/PL) |

## Ubiquitous Language
- **Lesson outcome**: what Community is told about a lesson; only held lessons get a review request today.
- **Review request**: the invitation to review a held lesson.
- **Review**: a 1-5 rating with a comment.
- **Review window**: 14 days after the lesson.
- **Rating**: the average of the tutor's last 50 reviews.

## Business Decisions
### Only held lessons get a review request (to re-confirm)
- **Given** a lesson outcome "student no-show", **When** it's reported, **Then** no review is requested.
### Review window
- **Given** a lesson held on 2026-11-01, **When** the student submits a review on 2026-11-16 (15 days later), **Then** the review is rejected. (Is day 14 inclusive?)
### Rating
- **Given** a tutor with 50 reviews averaging 4.7, **When** a 51st review of 5 arrives, **Then** the rating is the average of the last 50 (the oldest drops out) and Tutor Rating Updated is published.

## Assumptions
- Community hides abusive comments (to re-confirm; needs a rule).

## Verification Metrics
- Changing review eligibility (e.g. allowing reviews after late cancellations) needs no change outside Reviews.

## Open Questions
- Does an upheld complaint or a refund remove a review or affect the rating?
- Does a hidden comment's rating still count?
