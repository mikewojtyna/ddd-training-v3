# Bounded Context Canvas: TrustAndSafety

<!-- Pre-filled by discover-bounded-contexts (2026-11-05) via bounded-context-scaffolder, canvases only: no plan and no code yet. Source: docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml. Items marked (to re-confirm) were answered on the spot by the expert. -->

## Name
TrustAndSafety

## Purpose
Answers: **may this tutor teach on TutorHub?** The Trust & Safety team vets applicants once against per-country document rules and accepted qualifications. It is the single place that decides whether a tutor may teach, whatever the reason: an expired qualification or an upheld complaint. It ends today's situation where two teams suspended tutors independently and undid each other's decisions.

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
- Approver (proposed): receives applications and decides whether a tutor progresses to teaching.
- Enforcer (proposed): its eligibility decision is enforced by Scheduling (hidden from search, no new bookings).

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor | Command | Apply as tutor / Upload identity documents | |
| Complaints | Command | Report Upheld Complaint (tutor, lesson, category, severity) | In T&S's own terms: T&S defines the categories (to re-confirm) |
| (clock) | Event | Qualification Expired | Own rule on qualification validity |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Tutor Approved | Event | Scheduling | n/a for events |
| Tutor Approved (with country) | Event | Payouts | n/a for events (to re-confirm) |
| Tutor Suspended | Event | Scheduling | n/a for events |
| Tutor Reinstated (proposed event) | Event | Scheduling | n/a for events |

## Ubiquitous Language
- **Application**: a tutor's request to teach, with identity documents and qualifications.
- **Qualification**: a certificate that counts for teaching on TutorHub; it can expire.
- **Approval**: the one-time decision that the tutor may teach (pivotal event Tutor Approved).
- **Suspension**: the decision that a tutor may no longer teach; their profile is hidden from search and they can't receive new bookings.
- **Conduct report / upheld complaint**: Complaints' report that a complaint was upheld against a tutor, with a category (safety, conduct, teaching quality) and a severity.
- **Reinstatement**: lifting a suspension (proposed, e.g. after renewing a certificate).

## Business Decisions
### Approval (needs GWT)
- Expert's words: "which documents each country needs and which qualifications count", "approval happens once". Needs concrete per-country examples.
### Suspension on expired qualification (needs GWT)
- Given an approved tutor whose qualification expires, when the expiry date passes, then the tutor is suspended. Confirm whether this is immediate or has a grace period.
### Suspension on upheld complaint
- **Given** an approved tutor, **When** a conduct report with category "safety" arrives, **Then** the tutor is suspended immediately (to re-confirm).
- Other categories: "depend on severity and history". Needs GWT with concrete thresholds.
### One owner
- A tutor suspended after a complaint is not reinstated automatically when a certificate is renewed (the incident the expert described). Needs GWT for reinstatement.

## Assumptions
- Already booked lessons and credited earnings are not touched automatically by a suspension (to re-confirm).
- "Tutor Reinstated" is a real business event (to re-confirm).

## Verification Metrics
- No tutor reactivated by one reason while still suspended for another.

## Open Questions
- What decides reinstatement: a fixed duration, a manual review, or never?
- Is there an "Application Rejected" outcome, and what are its rules?
