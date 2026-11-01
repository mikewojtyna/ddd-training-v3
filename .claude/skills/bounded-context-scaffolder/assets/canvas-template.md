# Bounded Context Canvas: <Name>

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become aggregates and tests only if you write them as strict Given/When/Then examples with concrete values.
-->

## Name
<Name, exactly as it should appear in @BoundedContext (and in the CML model, if there is one)>

## Purpose
<What benefits does this context provide, and how? Describe it from a business perspective.>

## Strategic Classification
- Domain: <core | supporting | generic | other>
- Business model: <revenue | engagement | compliance | cost reduction>
- Evolution: <genesis | custom built | product | commodity>

## Domain Roles
<!-- Pick traits from docs/model-traits-worksheet.md -->
- <e.g. Execution Model>: <why>

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| <Rider app> | Command | <Unlock bike> | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| <Rental ended> | Event | <Pricing> | n/a for events |

## Ubiquitous Language
- **<Term>**: <definition>

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### <Rule name>
- **Given** <state>
- **When** <command or event>
- **Then** <outcome: event recorded, or rejection>

## Assumptions
- <unverified assumption>

## Verification Metrics
- <metric that would (in)validate this context's boundaries>

## Open Questions
- <question>
