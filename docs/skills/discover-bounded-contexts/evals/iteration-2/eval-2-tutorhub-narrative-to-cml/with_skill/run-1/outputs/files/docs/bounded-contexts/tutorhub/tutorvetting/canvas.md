# Bounded Context Canvas: TutorVetting

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (bounded-context-scaffolder, canvas only, no plan or code) from the TutorHub discovery session on 2026-11-05:
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Please review before any plan or code is generated. System package for later scaffolding: tutorhub (new system).
-->

## Name
TutorVetting

<!-- Matches `BoundedContext TutorVetting` in the CML. -->

## Purpose
Answers one question: **may this tutor teach on TutorHub?** Trust & Safety vets every applicant once (identity documents and qualifications, by country), approves them, and stays the single place that suspends and reinstates tutors, whatever the reason. This protects students and the marketplace's reputation and ends today's "mess" where two teams suspended and reactivated tutors on their own.

## Strategic Classification
- Domain: not decided (open question; trust is central to a marketplace)
- Business model: compliance (assumption, to confirm)
- Evolution: custom built (assumption, to confirm)

## Domain Roles
- Approver: decides whether an applicant progresses to selling lessons.
- Enforcer: makes Scheduling close a suspended tutor's calendar, Tutor Catalog hide them and Payouts hold their money.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor (app) | Command | Apply as tutor | Starts the application |
| Tutor (app) | Command | Upload identity documents | Which documents depends on the country |
| Trust & Safety (back office) | Command | Verify qualifications / Approve tutor | |
| Tutor (app) | Command | Renew qualification | Lifts an expiry suspension |
| Trust & Safety (back office) | Command | Reinstate tutor | For complaint suspensions, T&S decides |
| Complaints | Event | Complaint Upheld (against tutor X) | `ComplaintUpheld`, Complaints PL; we translate it (ACL) into a suspension |
| (clock) | Event | Qualification expiry date reached | Produces Qualification Expired |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Open tutor calendar (on approval) / Close tutor calendar (on suspension) / Reopen tutor calendar (on reinstatement) | Command | Scheduling (`TutorCalendar` OHS) | yes |
| Tutor Approved (with verified subjects), Qualification Expired, Tutor Suspended (reason), Tutor Reinstated | Event | Tutor Catalog (`TutorStanding`) | n/a for events |
| Tutor Suspended, Tutor Reinstated | Event | Payouts (`TutorSuspension`) | n/a for events |

## Ubiquitous Language
- **Application**: a person's request to become a tutor, with identity documents and qualifications.
- **Identity documents**: the documents a country requires to prove who the applicant is.
- **Verified qualification**: a qualification Trust & Safety accepted; it covers one or more subjects and may have an expiry date.
- **Approved tutor**: an applicant who passed vetting; approval happens once.
- **Suspension**: the tutor may not teach (hidden from search, no bookings, future lessons cancelled, payouts held). Has one or more **reasons**: expired qualification, upheld complaint.
- **Reinstatement**: lifting a suspension once every reason is cleared.

## Business Decisions
### Suspend on expired qualification (needs concrete values)
- **Given** an approved tutor whose qualification expires on <date>
- **When** that date is reached
- **Then** Tutor Suspended (reason: qualification expired) is recorded and the tutor's calendar is closed
- *Needs GWT confirmation: which qualifications expire, and is the tutor suspended for all subjects or only the affected one?*

### Suspend automatically on upheld complaint
- **Given** an approved, active tutor X
- **When** Complaint Upheld against tutor X is received
- **Then** Tutor Suspended (reason: complaint upheld) is recorded and the tutor's calendar is closed

### Renewal lifts only the expiry reason
- **Given** tutor X suspended for both an expired qualification and an upheld complaint
- **When** the qualification is renewed
- **Then** the expiry reason is cleared, but tutor X stays suspended (no Tutor Reinstated)

### Reinstate when every reason is cleared
- **Given** tutor X suspended only for an expired qualification
- **When** the qualification is renewed
- **Then** Tutor Reinstated is recorded and the calendar is reopened

### Country document rules (vague)
- "Which documents each country needs and which qualifications count" are Trust & Safety's rules. *Needs concrete GWT examples per country before modelling.*

## Assumptions
- Approval opens the tutor's calendar through Scheduling's API (confirmed).
- Business model and evolution above are assumptions.

## Verification Metrics
- No tutor is reinstated while any suspension reason is still open.
- No other context suspends or reinstates tutors.

## Open Questions
- Is a qualification-expiry suspension per subject or for the whole tutor?
- What do Tutor Catalog and Payouts need on expiry (unlist the subject only, or hide the tutor)?
- How long may a tutor take to renew before the account is closed, if ever?
