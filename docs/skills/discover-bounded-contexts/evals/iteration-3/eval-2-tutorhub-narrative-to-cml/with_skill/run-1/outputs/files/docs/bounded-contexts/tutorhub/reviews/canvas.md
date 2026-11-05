# Bounded Context Canvas: Reviews

<!--
Pre-filled by discover-bounded-contexts on 2026-11-05 (report: docs/context-discovery/tutorhub/2026-11-05-event-storming/discovery.md;
map: src/main/cml/TutorHub-Context-Map.cml). Based on the ddd-crew Bounded Context Canvas v5.
Status: draft for review. No plan or code yet. bounded-context-scaffolder builds code only from rules written as strict Given/When/Then.
Rule references (R<round>-<answer>) point to the Q&A in the discovery report.
-->

## Name
Reviews

## Purpose
Answers: **How do students rate this tutor?** Collects students' reviews of held lessons and maintains each tutor's rating, which students see in search and Pricing uses for the commission tier (R2-3).

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md; suggested by discovery, to confirm -->
- Analysis/Audit Model (suggested): monitors lesson quality through reviews.
- Engagement Context (suggested): ratings help students choose tutors.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Scheduling | Event | Lesson Held | Scheduling [U,S,PL]->[D,C,ACL] Reviews : HeldLessons |
| Student (app) | Command | Submit review (1-5 + comment) |  |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Tutor Rating Updated | Event | Pricing (and the search view) | n/a for events |
| Review Requested (invitation) | Event | Student | n/a |

## Ubiquitous Language
- **Review**: a 1-5 rating with a comment for a held lesson
- **Review window**: 14 days after the lesson
- **Tutor rating**: average of the tutor's last 50 reviews

## Business Decisions
<!-- In the expert's words from discovery. None is a strict Given/When/Then yet: each needs a concrete example before bounded-context-scaffolder can delegate it. -->
### Only held lessons
- Expert: "Only lessons that actually happened can be reviewed" (R2-3)
- Status: **needs Given/When/Then** (write GWT)

### 14-day window
- Expert: "A student rates the lesson 1-5 with a comment, up to 14 days after it." (R2-3)
- Status: **needs Given/When/Then** (concrete; write GWT)

### Rating
- Expert: "The rating is the average of the last 50 reviews." (R2-3)
- Status: **needs Given/When/Then** (concrete; write GWT)

## Assumptions
- none recorded

## Verification Metrics
- Rating changes reach Pricing before the next quote.

## Open Questions
- One review per lesson? Can it be edited or moderated (e.g. after a complaint)?
- Strategic classification.
