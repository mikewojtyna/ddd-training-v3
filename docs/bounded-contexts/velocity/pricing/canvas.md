# Bounded Context Canvas: Pricing

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values. ddd-tactical-building-blocks then decides whether each one needs an aggregate, a domain service, a policy or a factory.

PRE-FILLED by discover-bounded-contexts (canvas only, no plan, no code) on 2026-11-05 from
docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md and
src/main/cml/VeloCity-Context-Map-Discovery.cml. Expert quotes are verbatim. Review before scaffolding.
-->

## Name
Pricing

<!-- Matches `BoundedContext Pricing` in src/main/cml/VeloCity-Context-Map-Discovery.cml. -->

## Purpose
Calculates what a ride costs with one set of rules, used both for the live estimate the rider sees during the ride and for the final fee after it.

Key question (from discovery): **What does this ride cost?**

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
| Rental | Query | Estimate fee for a running session | live estimate |
| Settlement | Query | Final fee for a finished rental |  |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Fee estimate / Final fee | Query result | Rental / Settlement | n/a (answers) |

## Ubiquitous Language
- **Fee estimate**: "These fee applied are only for estimating the costs."
- **Free period**: First period of a ride without a fee (board: First Free Period Started/Ended).
- **Hourly tiers**: First, second, third hour, then fourth and subsequent hours (board).
- **Penalty**: Applied when max rental time is exceeded (board).
- **Zone fees**: Drop-zone fee and unauthorized-zone fee (board).

## Business Decisions
<!-- Rules marked "Needs GWT" or with "?" need concrete values before scaffolding. -->
### Same rules for estimate and final fee
- Needs GWT: "The same rules" (needs concrete tariff values for GWT)

### Station and zone returns
- Needs GWT: "The same rules" except zone fees (needs fee amounts)

## Assumptions
- Boundaries and messages come from the 2026-11-05 discovery session; see the report for the expert's verbatim answers.

## Verification Metrics
- <to decide>

## Open Questions
- Tariff values (free period length, hourly fees, penalty, zone fees)?
- Which tariff applies if it changes during a ride?
