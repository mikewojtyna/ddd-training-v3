# Bounded Context Canvas: Settlement

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
(see also docs/bounded-context-canvas-v5.jpg)
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become aggregates and tests only if you write them as strict Given/When/Then examples with concrete values.

Source: saved from the user's inline description on 2026-11-01. Nothing was added except the structure;
sections the user didn't fill in are marked TODO.
-->

## Name
Settlement

## Purpose
Once a ride is priced, we charge the rider through their payment method, so that every ride eventually gets paid.

## Strategic Classification
- Domain: TODO (core | supporting | generic | other)
- Business model: TODO (revenue | engagement | compliance | cost reduction)
- Evolution: TODO (genesis | custom built | product | commodity)

## Domain Roles
<!-- Pick traits from docs/model-traits-worksheet.md -->
- TODO

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Pricing | Event | Ride Priced | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Charge Rider | Command | Payments | no: Settlement is conformist to Payments and uses its API directly |
| Debt Recorded | Event | DebtRecovery | n/a for events |

## Ubiquitous Language
- **Settlement**: the process of collecting money for one priced ride.
- **Charge Attempt**: one try to take money from the rider's payment method.
- **Outstanding Debt**: the unpaid amount left after all attempts fail.

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Retry failed charges, then record debt
As given (prose, not yet a strict Given/When/Then): if a charge fails, we retry a few times shortly after the ride, then record debt.

TODO: rewrite as Given/When/Then with concrete values (how many attempts, how long after the ride). See the suggestion in `plan.md`.

## Assumptions
- TODO

## Verification Metrics
- TODO

## Open Questions
- TODO
