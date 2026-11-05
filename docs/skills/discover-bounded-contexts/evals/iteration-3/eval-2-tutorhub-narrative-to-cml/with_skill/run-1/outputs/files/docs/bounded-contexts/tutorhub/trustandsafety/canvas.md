# Bounded Context Canvas: TrustAndSafety

<!--
Pre-filled by discover-bounded-contexts on 2026-11-05 (report: docs/context-discovery/tutorhub/2026-11-05-event-storming/discovery.md;
map: src/main/cml/TutorHub-Context-Map.cml). Based on the ddd-crew Bounded Context Canvas v5.
Status: draft for review. No plan or code yet. bounded-context-scaffolder builds code only from rules written as strict Given/When/Then.
Rule references (R<round>-<answer>) point to the Q&A in the discovery report.
-->

## Name
TrustAndSafety

## Purpose
Answers: **May this tutor teach?** Keeps students safe and the platform trustworthy by being the ONE place that decides whether a tutor may teach: vetting new tutors once, and suspending or reinstating them for any reason (expired qualification, upheld complaint). Fixes today's mess where two teams suspended tutors independently (R1-4).

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md; suggested by discovery, to confirm -->
- Approver: receives tutor applications and decides whether the tutor progresses to teaching.
- Enforcer (suggested): its suspension decision is carried out by Scheduling (hide offer, cancel lessons).

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor (app) | Command | Apply; upload identity documents; submit qualifications / renewed qualification |  |
| Complaints | Event | Complaint Upheld ("complaint upheld against tutor X") | Complaints [U,S,PL]->[D,C,ACL] TrustAndSafety : UpheldComplaints |
| T&S reviewer | Command | Verify qualifications; approve; reinstate after review |  |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Tutor Approved | Event | Scheduling | n/a for events |
| Tutor Suspended (reason) | Event | Scheduling | n/a for events |
| Tutor Reinstated | Event | Scheduling | n/a for events |

## Ubiquitous Language
- **Tutor application**: a tutor applying to teach; vetted once
- **Identity documents**: documents required per country to prove identity
- **Qualification**: a credential that counts for teaching; has an expiry
- **Approved**: the tutor passed vetting; happens only once
- **Suspended**: the tutor may not teach; carries a reason (expired qualification | upheld complaint)
- **Reinstated**: the tutor may teach again; only when no suspension reason is left

## Business Decisions
<!-- In the expert's words from discovery. None is a strict Given/When/Then yet: each needs a concrete example before bounded-context-scaffolder can delegate it. -->
### Approval happens once
- Expert: "A tutor is approved only once." (R1-1)
- Status: **needs Given/When/Then** (concrete enough; needs a GWT example)

### Required documents per country
- Expert: "they have their own rules for which documents are needed per country and which qualifications count" (R1-1)
- Status: **needs Given/When/Then** (vague: needs the actual list per country and a GWT example)

### Expired qualification suspends
- Expert: "an expired qualification, which stays a suspension until it's renewed" (R1-4)
- Status: **needs Given/When/Then** (needs GWT with concrete dates)

### Upheld complaint suspends
- Expert: "Every complaint upheld against a tutor suspends them, and Trust & Safety decides that suspension." (R4-5)
- Status: **needs Given/When/Then** (needs GWT)

### Reinstatement only when no reason is left
- Expert: "It ends only when Trust & Safety reviews the case and reinstates the tutor" (R4-5); renewing a certificate must not lift a complaint suspension (R1-4 incident)
- Status: **needs Given/When/Then** (needs GWT: suspended for complaint + renewed qualification -> still suspended)

## Assumptions
- Suspension and reinstatement are one decision owned here; the other contexts only react to the events.

## Verification Metrics
- No tutor is ever reactivated while a suspension reason is still open.

## Open Questions
- What qualifies as a "renewed qualification", and who verifies it?
- Strategic classification.
