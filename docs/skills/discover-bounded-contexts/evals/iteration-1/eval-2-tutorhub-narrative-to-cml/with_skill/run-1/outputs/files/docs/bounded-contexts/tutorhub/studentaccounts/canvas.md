# Bounded Context Canvas: StudentAccounts

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
StudentAccounts

<!-- Matches `BoundedContext StudentAccounts` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Lets students register and add a payment card, and is the single owner of whether a student may book: a student with an unpaid lesson is blocked until the debt is settled.

The one question this context answers: **May this student book?**

## Strategic Classification
- Domain: supporting ("just has to work")
- Business model: revenue (protects against unpaid lessons)
- Evolution: custom built

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Approver: answers whether a student may book

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Student (app) | Command | Register / Add or update payment card | card stored at Stripe (Conformist to Stripe, CardRegistration) |
| Billing | Command | Report lesson unpaid (student S, lesson L) | Billing speaks our terms (StudentPaymentStanding) |
| Billing | Command | Report outstanding payment settled |  |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Student Blocked / Student Unblocked | Event | Scheduling (StudentStanding) | n/a |
| Payment Card Added / Updated (card reference) | Event | Billing (StudentPaymentStanding) | n/a |

## Ubiquitous Language
- **Student**: a registered person who books lessons
- **Payment card**: the student's card, held at Stripe; we keep only the reference
- **Blocked**: the student may not book new lessons until outstanding payments are settled
- **Unpaid lesson**: a lesson Billing could not charge after its retries

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Block on unpaid lesson
- **Given** Given an active student S
- **When** Billing reports lesson L unpaid
- **Then** Student Blocked is recorded and S can't book

### Unblock on settlement
- **Given** Given student S blocked for unpaid lesson L only
- **When** Billing reports the outstanding payment for L settled
- **Then** Student Unblocked is recorded

### No onboarding rules
(needs Given/When/Then) "nothing special for onboarding: they register and add a card" (expert round 2).

## Assumptions
- The student's card reference lives here, not in Billing (critic note, accepted).

## Verification Metrics
- No booking accepted for a blocked student.

## Open Questions
- Can a complaint upheld against a student also block them?
- What happens to already-booked lessons of a newly blocked student?
