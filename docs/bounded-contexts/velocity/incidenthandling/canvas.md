# Bounded Context Canvas: IncidentHandling

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values. ddd-tactical-building-blocks then decides whether each one needs an aggregate, a domain service, a policy or a factory.

PRE-FILLED by discover-bounded-contexts (canvas only, no plan, no code) on 2026-11-05 from
docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md and
src/main/cml/VeloCity-Context-Map-Discovery.cml. Expert quotes are verbatim. Review before scaffolding.
-->

## Name
IncidentHandling

<!-- Matches `BoundedContext IncidentHandling` in src/main/cml/VeloCity-Context-Map-Discovery.cml. -->

## Purpose
Handles incidents reported by riders or raised from bike faults, from triage through resolution, and asks maintenance to act on fault incidents.

Key question (from discovery): **What went wrong, and is it resolved?**

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
| Rider / support | Command | Report incident |  |
| Rental | Event | Bike Fault Reported / Bike Locking Failed | "To incidents first." |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Maintenance request | Command | Fleet Maintenance | yes |

## Ubiquitous Language
- **Incident**: Reported → type detected → assigned to agent → accepted / rejected → escalated → resolved; can be reopened (board).

## Business Decisions
<!-- Rules marked "Needs GWT" or with "?" need concrete values before scaffolding. -->
### Fault incidents go to maintenance
- **Given** a fault incident
- **When** it is triaged
- **Then** maintenance is requested (needs concrete triage rules)

## Assumptions
- Boundaries and messages come from the 2026-11-05 discovery session; see the report for the expert's verbatim answers.

## Verification Metrics
- <to decide>

## Open Questions
- Does resolving an incident change anything elsewhere (refund, fee waiver)?
- Escalation and assignment rules?
