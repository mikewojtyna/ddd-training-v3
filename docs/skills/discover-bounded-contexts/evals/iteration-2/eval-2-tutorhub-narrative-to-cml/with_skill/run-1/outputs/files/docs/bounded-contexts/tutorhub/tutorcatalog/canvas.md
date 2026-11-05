# Bounded Context Canvas: TutorCatalog

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (bounded-context-scaffolder, canvas only, no plan or code) from the TutorHub discovery session on 2026-11-05:
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Please review before any plan or code is generated. System package for later scaffolding: tutorhub (new system).
-->

## Name
TutorCatalog

<!-- Matches `BoundedContext TutorCatalog` in the CML. Called "Tutor Profiles" earlier in the session. -->

## Purpose
Answers: **what does each tutor offer, and which tutors can a student find?** Approved tutors manage their own profile (subjects, description) and hourly rate; students search by subject, price (the hourly rate), rating and free slots. No team owns it; it's tutor self-service.

## Strategic Classification
- Domain: supporting or generic (critic's view; not confirmed by the expert)
- Business model: engagement (assumption)
- Evolution: product / commodity (assumption: profiles and search are well understood)

## Domain Roles
- Specification/Draft Model: the tutor's published offer.
- (Search is a read view with no rules.)

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Tutor (app) | Command | Publish profile / Set hourly rate / Change hourly rate | EUR, any time |
| Student (app) | Query | Search tutors (subject, price, rating, free slots) | No business rules |
| Tutor Vetting | Event | Tutor Approved (verified subjects), Qualification Expired, Tutor Suspended, Tutor Reinstated | `TutorStanding`, translated by our ACL |
| Reviews | Event | Tutor Rating Updated | `TutorRating`, for display and filtering |
| Scheduling | Query | Free slots of tutor X | `FreeSlots` OHS, for the search view |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Register tutor hourly rate | Command | Pricing (`TutorRateRegistration` OHS) | yes |
| Get free slots | Query | Scheduling (`FreeSlots` OHS) | yes |

## Ubiquitous Language
- **Profile**: what a tutor publishes about themselves: subjects, description.
- **Subject**: something a tutor teaches; may be listed only if Trust & Safety verified it.
- **Hourly rate**: the tutor's own price per hour, in EUR, changeable at any time; the rate valid at booking applies.
- **Listing**: a tutor visible in search; suspended tutors are not listed.
- **Price (in search)**: the tutor's hourly rate.

## Business Decisions
### Only verified subjects can be listed
- **Given** tutor X whose verified subjects are {Maths}
- **When** tutor X adds Physics to the profile
- **Then** the change is rejected

### Suspended tutors are hidden
- **Given** listed tutor X
- **When** Tutor Suspended for X is received
- **Then** X no longer appears in search results (and reappears on Tutor Reinstated)

### Rate in EUR, no limits
- **Given** an approved tutor
- **When** the tutor sets the hourly rate to 35 EUR
- **Then** Hourly Rate Set (35 EUR) is recorded and registered with Pricing

## Assumptions
- Listing reflects Tutor Vetting's decision; the catalog doesn't decide who may teach.

## Verification Metrics
- Zero suspended tutors visible in search.

## Open Questions
- On Qualification Expired, unlist only the affected subject, or hide the whole tutor?
- Does the profile need anything else (languages, photo moderation)?
