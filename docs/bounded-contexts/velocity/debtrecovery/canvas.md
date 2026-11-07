# Bounded Context Canvas: DebtRecovery

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values. ddd-tactical-building-blocks then decides whether each one needs an aggregate, a domain service, a policy or a factory.

PRE-FILLED by discover-bounded-contexts (canvas only, no plan, no code) on 2026-11-05 from
docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md and
src/main/cml/VeloCity-Context-Map-Discovery.cml. Expert quotes are verbatim. Review before scaffolding.
-->

## Name
DebtRecovery

<!-- Matches `BoundedContext DebtRecovery` in src/main/cml/VeloCity-Context-Map-Discovery.cml. -->

## Purpose
Runs a recovery process for unsettled rentals, with its own actions and schedule, and confirms to Settlement when the debt is recovered.

Key question (from discovery): **How do we recover what's owed?**

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
| Settlement | Event | Rental Unsettled | starts the recovery process |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Confirm rental settled | Command | Settlement | yes |

## Ubiquitous Language
- **Recovery process**: Started, paused, resumed, succeeded, failed (board). Has its own rules: "Yes" (actions and schedule).

## Business Decisions
<!-- Rules marked "Needs GWT" or with "?" need concrete values before scaffolding. -->
### Recovery actions and schedule
- Needs GWT: own rules confirmed ("Yes"), not yet specified (needs GWT)

## Assumptions
- Boundaries and messages come from the 2026-11-05 discovery session; see the report for the expert's verbatim answers.

## Verification Metrics
- <to decide>

## Open Questions
- Which actions, on what schedule?
- What pauses and resumes a recovery?
- What happens after recovery fails?
- Does it charge the card itself, or does the rider pay another way?
