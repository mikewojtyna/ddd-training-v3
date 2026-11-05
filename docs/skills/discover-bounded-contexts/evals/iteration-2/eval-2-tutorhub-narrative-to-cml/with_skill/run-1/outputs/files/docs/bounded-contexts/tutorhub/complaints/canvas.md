# Bounded Context Canvas: Complaints

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (bounded-context-scaffolder, canvas only, no plan or code) from the TutorHub discovery session on 2026-11-05:
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Please review before any plan or code is generated. System package for later scaffolding: tutorhub (new system).
-->

## Name
Complaints

<!-- Matches `BoundedContext Complaints` in the CML. -->

## Purpose
Answers: **is this complaint justified, and what remedy follows?** The Complaints team investigates complaints from students or tutors, filed anytime (even weeks later), and decides the outcome: rejected, a refund, upheld against the tutor, or both. It decides but doesn't execute: Billing moves the money and Trust & Safety decides on suspension.

## Strategic Classification
- Domain: supporting (assumption)
- Business model: engagement / compliance (assumption)
- Evolution: custom built (assumption)

## Domain Roles
- Approver: decides whether a complaint leads to a remedy.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student or tutor (app) | Command | File complaint | Anytime, including disputed no-shows |
| Complaints team (back office) | Command | Record investigation / decide outcome | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Lesson record (took place? who joined, when?) | Query | Scheduling (`LessonRecord` OHS) | yes |
| What was charged for lesson L? | Query | Billing (`Refunds` OHS) | yes |
| Refund lesson L, amount A | Command | Billing (`Refunds` OHS) | yes |
| Complaint Upheld (against tutor X) | Event | Tutor Vetting (`ComplaintUpheld`) | n/a for events |

## Ubiquitous Language
- **Complaint**: a student's or tutor's claim that something went wrong with a lesson or a person.
- **Investigation**: the Complaints team's review of the evidence (lesson record, charges).
- **Outcome**: rejected, refund, upheld against the tutor, or refund and upheld.
- **Refund decision**: "refund lesson L, amount A"; executed by Billing.
- **Upheld complaint**: a finding against tutor X; Trust & Safety turns it into a suspension.

## Business Decisions
### Outcome refund and upheld
- **Given** a complaint about lesson L charged 46.00 EUR, investigated and found justified against tutor X
- **When** the team decides "refund and upheld"
- **Then** Refund Decided (L, 46.00 EUR) and Complaint Upheld (X) are recorded

### Rejected complaint has no remedy
- **Given** an investigated complaint
- **When** the team decides "rejected"
- **Then** Complaint Rejected is recorded and no refund or upheld event is published

### Refund amount rules (vague)
- *Needs GWT: may amount A exceed what was charged? Is it free, or a rule (full price, fee only)?*

## Assumptions
- Complaints never suspends tutors and never moves money itself (confirmed).

## Verification Metrics
- Time from Complaint Filed to outcome.

## Open Questions
- Are there deadlines, escalations or reopening rules for investigations?
- What remedies exist when a tutor complains about a student?
- If a disputed no-show is overturned, does that correct Pricing's lesson count or Billing's charge, and who issues it?
- Can a refund fail, and must Complaints hear about it?
