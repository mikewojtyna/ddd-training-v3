# Bounded Context Canvas: Payments

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values. ddd-tactical-building-blocks then decides whether each one needs an aggregate, a domain service, a policy or a factory.

PRE-FILLED by discover-bounded-contexts (canvas only, no plan, no code) on 2026-11-05 from
docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md and
src/main/cml/VeloCity-Context-Map-Discovery.cml. Expert quotes are verbatim. Review before scaffolding.
-->

## Name
Payments

<!-- Matches `BoundedContext Payments` in src/main/cml/VeloCity-Context-Map-Discovery.cml. -->

## Purpose
Executes charges and verifies payment methods in front of the external card processor, including retrying failed charges.

Key question (from discovery): **Did the charge go through, after retries?**

## Strategic Classification
- Domain: <to decide: not discussed in discovery>
- Business model: <to decide>
- Evolution: <to decide>

## Domain Roles
<!-- Pick traits from docs/model-traits-worksheet.md. Not discussed in discovery. -->
- <to decide>

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Rider Accounts | Command | Verify payment method |  |
| Settlement | Command | Charge |  |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Card processing | Command | PaymentProcessor (external) | yes |
| Payment Method Verified / Not Verified | Event | Rider Accounts | n/a for events |
| Payment Succeeded / Payment Failed | Event | Settlement | n/a; published after retries |

## Ubiquitous Language
- **Charge retry**: "Retries are part of it" (Payments' own retries of one charge).
- **Payment Failed**: Published only when Payments' retries are exhausted.

## Business Decisions
<!-- Rules marked "Needs GWT" or with "?" need concrete values before scaffolding. -->
### Charge retries
- **Given** a charge declined by the processor
- **When** Payments retries (count/interval: ?)
- **Then** Payment Succeeded, or Payment Failed when retries run out

## Assumptions
- Boundaries and messages come from the 2026-11-05 discovery session; see the report for the expert's verbatim answers.

## Verification Metrics
- <to decide>

## Open Questions
- Retry count and interval?
- Does Debt Recovery charge the card itself (would add Payments -> DebtRecovery)?
