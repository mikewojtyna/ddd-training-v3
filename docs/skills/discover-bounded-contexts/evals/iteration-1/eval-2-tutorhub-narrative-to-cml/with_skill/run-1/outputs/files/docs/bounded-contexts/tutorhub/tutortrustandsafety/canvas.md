# Bounded Context Canvas: TutorTrustAndSafety

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
TutorTrustAndSafety

<!-- Matches `BoundedContext TutorTrustAndSafety` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Makes sure every tutor students meet is vetted and allowed to teach. Vets applicants (identity documents per country, qualifications), approves them once, watches qualification expiry, and is the single place that suspends or reinstates a tutor, whatever the reason. Students' trust in vetted tutors is TutorHub's edge. Also hosts the tutor's self-service profile after approval (a convenience: the profile has no rules of its own).

The one question this context answers: **May this tutor teach?**

## Strategic Classification
- Domain: core (with Reviews): "our edge is that students trust our tutors: vetted, with honest ratings"
- Business model: engagement (trust that keeps students booking)
- Evolution: custom built

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Approver: decides whether an applicant progresses to approved tutor
- Enforcer: its "suspended" fact makes Scheduling refuse bookings and cancel lessons and hides the profile from search

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor (app) | Command | Apply as tutor / Upload identity documents |  |
| Trust & Safety staff | Command | Verify qualifications / Approve tutor |  |
| Complaints | Command | Report complaint upheld against tutor X | Complaints speaks our terms (UpheldComplaintIntake); we decide whether to suspend |
| Tutor (app) | Command | Publish profile | self-service, no rules |
| (clock) | Event | Qualification validity date passed | -> Qualification Expired |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Tutor Approved | Event | Scheduling | n/a for events (published language) |
| Tutor Suspended (reason) | Event | Scheduling; search read model | n/a |
| Tutor Reinstated | Event | Scheduling; search read model | n/a |
| Tutor Profile Published | Event | search read model | n/a |
| Tutor country | Event/Query | Payouts (TutorCountry) | n/a |

## Ubiquitous Language
- **Tutor application**: a request to teach, with identity documents and qualifications
- **Identity documents**: documents required per country to prove who the tutor is
- **Qualification**: a certificate or degree that counts for teaching a subject; has a validity date
- **Approved**: vetted once; the tutor may publish a profile, a rate and slots
- **Suspended**: the tutor may not teach: profile hidden, no new bookings, future lessons cancelled; reasons: qualification expired, complaint upheld
- **Reinstated**: a suspension lifted, e.g. after a renewed certificate
- **Tutor profile**: the tutor's own description after approval; self-service

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Approval requires vetting
(needs Given/When/Then) which documents per country and which qualifications count (Trust & Safety's own rules).

### Suspension on expired qualification
- **Given** Given an approved tutor whose only maths qualification is valid until 2026-11-04
- **When** the date 2026-11-05 is reached
- **Then** Qualification Expired and Tutor Suspended (reason: qualification expired) are recorded

### One owner of suspension
- **Given** Given an approved tutor and a report "complaint upheld against tutor T" from Complaints
- **When** Trust & Safety decides
- **Then** Trust & Safety records Tutor Suspended (reason: complaint upheld); no other context suspends tutors (needs the exact decision rule: always suspend, or weigh the case?)

### Reinstatement
(needs Given/When/Then) a suspended tutor is reinstated after renewing a certificate (expert, round 1).

## Assumptions
- The profile lives here only for convenience (expert round 3: option b); it must not grow rules.
- Tutor Approved is the pivot; Tutor Profile Published is not.

## Verification Metrics
- Zero cases of a tutor being suspended by one team and reactivated by another (the old mess).
- Time from application to approval.

## Open Questions
- Does an upheld complaint always lead to suspension, or is it weighed (warning, temporary vs permanent)?
- What lifts a complaint-based suspension, and who decides?
- Missing events: Tutor Application Rejected, Qualification Renewed, Tutor Reinstated.
