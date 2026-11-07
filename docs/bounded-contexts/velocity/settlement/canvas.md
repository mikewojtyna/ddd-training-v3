# Bounded Context Canvas: Settlement

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values. ddd-tactical-building-blocks then decides whether each one needs an aggregate, a domain service, a policy or a factory.

PRE-FILLED by discover-bounded-contexts (canvas only, no plan, no code) on 2026-11-05 from
docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md and
src/main/cml/VeloCity-Context-Map-Discovery.cml. Expert quotes are verbatim. Review before scaffolding.
-->

## Name
Settlement

<!-- Matches `BoundedContext Settlement` in src/main/cml/VeloCity-Context-Map-Discovery.cml. -->

## Purpose
Turns a finished ride into a bill, gets it paid, and decides when a rental is settled or unsettled, including asking Rider Accounts to block or lift the block.

Key question (from discovery): **What does the rider owe for this rental, and is it settled?**

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
| Rental | Event | Rental Session Ended |  |
| Pricing | Query (answer) | Final fee |  |
| Payments | Event | Payment Succeeded / Payment Failed | after Payments' own retries |
| Debt Recovery | Command | Confirm rental settled | "debt recovery confirms that rental is settled by sending a command to the settlement" |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Final fee | Query | Pricing | yes |
| Charge | Command | Payments | yes |
| Block account (reason: debt) / Lift block | Command | Rider Accounts | yes |
| Rental Settled / Rental Unsettled | Event | Debt Recovery (and others) | n/a for events |

## Ubiquitous Language
- **Bill**: What the rider owes for one rental.
- **Unsettled**: "It retries. After exceeding max results, the rental is unsettled."
- **Settled**: Paid, or confirmed settled by Debt Recovery.

## Business Decisions
<!-- Rules marked "Needs GWT" or with "?" need concrete values before scaffolding. -->
### Unsettled after max re-charges
- **Given** a rental whose charge failed (after Payments' retries)
- **When** Settlement's re-charges exceed the maximum (value: ?)
- **Then** Rental Unsettled; account blocked with reason debt

### Lift block when settled
- **Given** an unsettled rental, account blocked for debt
- **When** Debt Recovery confirms the rental is settled
- **Then** Rental Settled; block lifted (several unsettled rentals: ?)

## Assumptions
- Boundaries and messages come from the 2026-11-05 discovery session; see the report for the expert's verbatim answers.

## Verification Metrics
- <to decide>

## Open Questions
- Maximum number of re-charges and their schedule?
- With several unsettled rentals, is the block lifted only when all are settled?
