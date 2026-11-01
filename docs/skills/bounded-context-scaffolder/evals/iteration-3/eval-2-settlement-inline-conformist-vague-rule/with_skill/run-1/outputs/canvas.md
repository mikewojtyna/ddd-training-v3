# Bounded Context Canvas: Settlement

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become aggregates and tests only if you write them as strict Given/When/Then examples with concrete values.

Saved on 2026-11-01 from the user's inline request. Content is as the user wrote it, only put into the template structure.
Sections marked "not provided yet" were empty in the request; please fill them in where you can.
-->

## Name
Settlement

## Purpose
Once a ride is priced, we charge the rider through their payment method, so that every ride eventually gets paid.

## Strategic Classification
<!-- not provided yet -->
- Domain: <core | supporting | generic | other>
- Business model: <revenue | engagement | compliance | cost reduction>
- Evolution: <genesis | custom built | product | commodity>

## Domain Roles
<!-- not provided yet. Pick traits from docs/model-traits-worksheet.md -->

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Pricing | Event | Ride Priced | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Charge Rider | Command | Payments | no: "we're conformist to Payments, skip the ACL and use their API directly" |
| Debt Recorded | Event | DebtRecovery | n/a for events |

## Ubiquitous Language
- **Settlement**: the process of collecting money for one priced ride.
- **Charge Attempt**: one try to take money from the rider's payment method.
- **Outstanding Debt**: the unpaid amount left after all attempts fail.

## Business Decisions
### Retry failed charges, then record debt
If a charge fails, we retry a few times shortly after the ride, then record debt.

Strict examples, given by the user in review on 2026-11-01:

- **Rule 1**: Given a priced ride of 12.50 PLN with 2 failed charge attempts, When the 3rd charge attempt fails, Then an Outstanding Debt of 12.50 PLN is recorded and Debt Recorded is published.
- **Rule 2**: Given a priced ride of 12.50 PLN with 1 failed charge attempt, When the 2nd charge attempt succeeds, Then the ride is settled and no Outstanding Debt is recorded.

<!-- "Shortly after the ride" (when retries happen) is still not a strict Given/When/Then example. See "Rules awaiting GWT" in plan.md. -->

## Assumptions
<!-- not provided yet -->

## Verification Metrics
<!-- not provided yet -->

## Open Questions
<!-- not provided yet -->
