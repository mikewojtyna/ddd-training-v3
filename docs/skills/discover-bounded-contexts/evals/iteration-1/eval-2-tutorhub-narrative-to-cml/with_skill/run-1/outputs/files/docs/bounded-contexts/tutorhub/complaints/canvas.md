# Bounded Context Canvas: Complaints

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
Complaints

<!-- Matches `BoundedContext Complaints` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Handles complaints from students or tutors at any time, investigates them and decides the remedy: reject, refund (full or partial), or uphold against the tutor. It decides; it never moves money or suspends tutors itself.

The one question this context answers: **Was something wrong, and what is the remedy?**

## Strategic Classification
- Domain: supporting ("just has to work")
- Business model: engagement
- Evolution: custom built

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Approver: decides whether a complaint leads to a remedy

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student / Tutor (app) | Command | File complaint about lesson L | anytime, even weeks later |
| Scheduling | Query | Lesson details (who taught whom, when) | LessonDetails, ACL |
| Billing | Event | Refund Issued | confirmation |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Refund lesson L, amount A | Command | Billing (Refunds) | yes (in Billing's terms) |
| Complaint upheld against tutor X | Command | TutorTrustAndSafety (UpheldComplaintIntake) | yes (in Trust & Safety's terms) |
| Complaint Rejected / Refund Decided / Complaint Upheld Against Tutor | Event | (complainant notification) | n/a |

## Ubiquitous Language
- **Complaint**: a grievance about a lesson, filed by a student or a tutor at any time
- **Investigation**: the Complaints team's examination of the case
- **Refund decision**: refund lesson L, amount A; may be partial
- **Upheld against tutor**: the outcome that the tutor was at fault; Trust & Safety decides what follows

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Outcome
(needs Given/When/Then) an investigation ends as rejected, refund, upheld against the tutor, or refund and upheld (needs the criteria).

### Refund amount
(needs Given/When/Then) Complaints decides the amount, which may be partial; Billing executes it (needs concrete examples).

## Assumptions
- Complaints never suspends tutors (single owner: TutorTrustAndSafety) and never moves money (Billing).

## Verification Metrics
- Time from complaint filed to outcome.

## Open Questions
- Can a complaint be upheld against a student, with what consequence?
- Investigation SLA, escalation, reopening?
