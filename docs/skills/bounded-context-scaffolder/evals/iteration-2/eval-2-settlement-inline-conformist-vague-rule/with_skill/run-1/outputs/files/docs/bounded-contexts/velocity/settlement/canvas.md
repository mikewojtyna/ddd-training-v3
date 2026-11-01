# Bounded Context Canvas: Settlement

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
(see also docs/bounded-context-canvas-v5.jpg)
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become aggregates and tests only if you write them as strict Given/When/Then examples with concrete values.

Saved from the inline description given in the conversation on 2026-11-01. Sections marked TODO were
not part of that description; fill them in when you can.
-->

## Name
Settlement

## Purpose
Once a ride is priced, we charge the rider through their payment method, so that every ride eventually gets paid.

## Strategic Classification
<!-- TODO: not given yet -->
- Domain: <core | supporting | generic | other>
- Business model: <revenue | engagement | compliance | cost reduction>
- Evolution: <genesis | custom built | product | commodity>

## Domain Roles
<!-- TODO: not given yet. Pick traits from docs/model-traits-worksheet.md (Execution Model looks like a candidate). -->

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Pricing | Event | Ride Priced | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Charge Rider | Command | Payments | no: we are conformist to Payments and use their API directly |
| Debt Recorded | Event | DebtRecovery | n/a for events |

## Ubiquitous Language
- **Settlement**: the process of collecting money for one priced ride.
- **Charge Attempt**: one try to take money from the rider's payment method.
- **Outstanding Debt**: the unpaid amount left after all attempts fail.

## Business Decisions
### Retry failed charges, then record debt
If a charge fails, we retry a few times shortly after the ride, then record debt.

Strict examples (confirmed in the conversation on 2026-11-01):
1. **Record debt after the 3rd failed charge attempt.** Given a priced ride of 12.50 PLN with 2 failed charge attempts, when the 3rd charge attempt fails, then an Outstanding Debt of 12.50 PLN is recorded and Debt Recorded is published.
2. **A successful retry settles the ride.** Given a priced ride of 12.50 PLN with 1 failed charge attempt, when the 2nd charge attempt succeeds, then the ride is settled and no Outstanding Debt is recorded.

<!-- TODO: "shortly after the ride" (when the 2nd and 3rd attempts are made) has no strict example yet. See plan.md. -->

## Assumptions
<!-- TODO -->

## Verification Metrics
<!-- TODO -->

## Open Questions
<!-- TODO -->
