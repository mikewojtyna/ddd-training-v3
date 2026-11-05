# Bounded Context Canvas: Complaints

<!--
Pre-filled by discover-bounded-contexts on 2026-11-05 (report: docs/context-discovery/tutorhub/2026-11-05-event-storming/discovery.md;
map: src/main/cml/TutorHub-Context-Map.cml). Based on the ddd-crew Bounded Context Canvas v5.
Status: draft for review. No plan or code yet. bounded-context-scaffolder builds code only from rules written as strict Given/When/Then.
Rule references (R<round>-<answer>) point to the Q&A in the discovery report.
-->

## Name
Complaints

## Purpose
Answers: **Was this complaint justified, and what is the remedy?** Investigates complaints filed by students or tutors at any time and decides the remedy: reject, refund, uphold against the tutor, or both. Announces decisions in its own terms and never suspends anyone itself (R1-4, R7-3).

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md; suggested by discovery, to confirm -->
- Approver (suggested): decides whether a complaint leads to a refund and/or a tutor sanction.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student or tutor (app) | Command | File complaint |  |
| Scheduling | Event | Lesson details (who, when, what happened) | Scheduling [U,S,PL]->[D,C,ACL] Complaints : LessonDetails |
| Billing | Event | What was charged | Billing [U,S,PL]->[D,C,ACL] Complaints : ChargeDetails |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Refund Decided ("refund lesson L, amount A") | Event | Billing | n/a for events |
| Complaint Upheld ("against tutor X") | Event | TrustAndSafety | n/a for events |
| Complaint Rejected | Event | (complainant) | n/a |

## Ubiquitous Language
- **Complaint**: a grievance about a lesson, filed by a student or a tutor at any time
- **Investigation**: the Complaints team's examination; no formal deadlines
- **Outcome**: rejected | refund | upheld against tutor | refund and upheld
- **Refund decision**: "refund lesson L, amount A"; Billing carries it out

## Business Decisions
<!-- In the expert's words from discovery. None is a strict Given/When/Then yet: each needs a concrete example before bounded-context-scaffolder can delegate it. -->
### Outcomes
- Expert: "rejected, refund, upheld against the tutor, or both refund and upheld" (R2-4)
- Status: **needs Given/When/Then** (write GWT per outcome)

### Refund amount decided here
- Expert: "Complaints decides the refund ('refund lesson L, amount A')" (R2-4)
- Status: **needs Given/When/Then** (vague: how is A determined? needs GWT)

### No student blocking
- Expert: "A complaint against a student can only end in a refund or a rejection today." (R4-5)
- Status: **needs Given/When/Then** (write GWT)

### Never suspends
- Expert: "Complaints should only say 'complaint upheld against tutor X'" (R1-4)
- Status: **needs Given/When/Then** (write GWT)

## Assumptions
- none recorded

## Verification Metrics
- No tutor suspended without Trust & Safety's decision.

## Open Questions
- Is the refund capped at what was charged?
- Strategic classification.
