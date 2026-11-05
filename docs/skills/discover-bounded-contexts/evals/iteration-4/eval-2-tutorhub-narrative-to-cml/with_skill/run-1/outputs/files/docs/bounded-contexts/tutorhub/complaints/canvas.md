# Bounded Context Canvas: Complaints

<!-- Pre-filled by discover-bounded-contexts (2026-11-05) via bounded-context-scaffolder, canvases only: no plan and no code yet. Source: docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml. Items marked (to re-confirm) were answered on the spot by the expert. -->

## Name
Complaints

## Purpose
Answers: **how is this complaint resolved?** The Complaints team investigates complaints from students or tutors, filed at any time, even weeks after a lesson. It decides the outcome: rejected, refund, upheld against the tutor, or both. It decides refund amounts and whether the tutor's share is taken back. It never suspends tutors or moves money itself: Trust & Safety and Billing do that.

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
- Approver (proposed): decides whether a complaint leads to a refund or an upheld report.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student / Tutor | Command | File complaint | Any time |
| Scheduling | Query | Lesson record snapshot (details, no-show record) | Read-only, taken at filing (to re-confirm) |
| Billing | Query | Charge and fee for a lesson | Read-only |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Refund Lesson (lesson, amount, take back tutor share yes/no) | Command | Billing | yes |
| Report Upheld Complaint (tutor, lesson, category, severity) | Command | TrustAndSafety | yes (T&S's categories) |

## Ubiquitous Language
- **Complaint**: a student's or tutor's report about a lesson or tutor.
- **Investigation**: reviewing the lesson record and the charge.
- **Outcome**: rejected, refund, upheld against the tutor, or refund and upheld.
- **Refund decision**: "refund lesson L, amount A", plus whether the tutor's share is taken back.
- **Upheld complaint**: a finding against a tutor, reported to Trust & Safety with a category (safety, conduct, teaching quality) and a severity defined by T&S.

## Business Decisions
### Refund only
- **Given** a complaint about lesson L charged at 46 EUR, **When** the investigation decides a full refund without upholding it against the tutor, **Then** Refund Decided (L, 46 EUR) is sent to Billing and nothing goes to Trust & Safety.
### Upheld and refund
- **Given** a complaint about lesson L, **When** it is upheld for conduct with a refund of 46 EUR and the tutor's share taken back, **Then** Refund Decided (L, 46 EUR, take back share) goes to Billing and an upheld-complaint report (conduct, severity) goes to Trust & Safety.
### When the tutor's share is taken back (needs GWT, to re-confirm)
- Is it decided case by case, or does every refund take back the tutor's share?

## Assumptions
- No-show disputes come in as ordinary complaints.

## Verification Metrics
- Suspension policy changes in Trust & Safety don't require changes here, unless a category is added or redefined.

## Open Questions
- What can a tutor's complaint lead to, e.g. a disputed no-show recorded against them? None of the four outcomes covers it.
- Is a "Complaint Resolved" event (carrying the outcome) needed on the board?
