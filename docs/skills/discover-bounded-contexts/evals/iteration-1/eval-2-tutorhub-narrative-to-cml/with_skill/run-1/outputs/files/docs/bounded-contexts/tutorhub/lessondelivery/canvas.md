# Bounded Context Canvas: LessonDelivery

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
LessonDelivery

<!-- Matches `BoundedContext LessonDelivery` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Runs the lesson itself through the third-party video provider, kept at arm's length, and decides from its webhooks whether the lesson happened and who showed up. Its outcome decides whether the student pays.

The one question this context answers: **Did the lesson happen, and who showed up?**

## Strategic Classification
- Domain: supporting
- Business model: revenue
- Evolution: custom built (wraps a bought video service)

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Gateway: sits at the edge towards the video provider and translates its messy webhook model (ACL)
- Execution Model: tracks the lesson session

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Scheduling | Event | Lesson Booked / Rescheduled / Cancelled | LessonAppointments |
| VideoProvider | Event | Room created / participant joined / left webhooks | ACL (VideoSessions); webhook model changed twice this year |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Video Room Opened | Event | (internal) | n/a |
| Lesson Started / Lesson Ended | Event | Billing, Reviews, Pricing | n/a |
| Student No-show Recorded / Tutor No-show Recorded | Event | Billing, Pricing | n/a |

## Ubiquitous Language
- **Video room**: the provider's room for one lesson, opened 5 minutes before the start
- **Lesson started / ended**: taken from the provider's webhooks
- **No-show**: a participant who didn't join within 10 minutes of the start; student or tutor

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Room opens early
- **Given** Given a lesson booked for 2026-11-10 10:00
- **When** the time is 09:55
- **Then** Video Room Opened is recorded

### Student no-show
- **Given** Given a lesson starting at 10:00 and the tutor joined at 09:58
- **When** the student hasn't joined by 10:10
- **Then** Student No-show Recorded

### Tutor no-show
- **Given** Given a lesson starting at 10:00 and the student joined at 10:00
- **When** the tutor hasn't joined by 10:10
- **Then** Tutor No-show Recorded

## Assumptions
- A separate small team looks after the video integration (expert, round 2).

## Verification Metrics
- A provider webhook change touches only the ACL, not the no-show rule.

## Open Questions
- Does a lesson that ends early (someone drops off) change the charge?
- What if both participants are late?
