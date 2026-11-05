# Bounded Context Canvas: Rental

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values. ddd-tactical-building-blocks then decides whether each one needs an aggregate, a domain service, a policy or a factory.

PRE-FILLED by discover-bounded-contexts (canvas only, no plan, no code) on 2026-11-05 from
docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md and
src/main/cml/VeloCity-Context-Map-Discovery.cml. Expert quotes are verbatim. Review before scaffolding.
-->

## Name
Rental

<!-- Matches `BoundedContext Rental` in src/main/cml/VeloCity-Context-Map-Discovery.cml. -->

## Purpose
Lets eligible riders reserve, unlock, ride and return bikes, at a station or in a zone, and records each rental session from start to end.

Key question (from discovery): **Who has which bike, and when?**

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
| Rider app | Command | Reserve bike / Unlock bike / Return bike |  |
| Rider Accounts | Query (answer) | Rider eligibility | asked before each rental |
| Fleet Maintenance | Event | Bike Out Of Service / Bike Back In Circulation | maintenance decides availability |
| Pricing | Query (answer) | Fee estimate | live estimate during the ride |
| City | Query (answer) | Zone data | external; assumption, unconfirmed |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Can this rider rent? | Query | Rider Accounts | yes |
| Estimate fee | Query | Pricing | yes |
| Zone lookup | Query | City | yes |
| Rental Session Started / Rental Session Ended | Event | Settlement (and others) | n/a for events |
| Bike Fault Reported / Bike Locking Failed | Event | Incident Handling | n/a for events |

## Ubiquitous Language
- **Reservation window**: "bike reserved has a reservation window which decides whether the bike is locked for this specific user."
- **Rental session**: Starts at Rental Session Started, which "initiates the ride"; ends when the bike is returned (station or zone) or the session is ended.
- **Location check**: "User location is required to check whether user is allowed to rent a bike." Runs before every rental.
- **Station return / zone return**: Two endings of the same ride; "The same rules" except zone fees (Pricing).

## Business Decisions
<!-- Rules marked "Needs GWT" or with "?" need concrete values before scaffolding. -->
### Reservation window
- **Given** a bike reserved by rider A, inside its reservation window (length: ?)
- **When** rider B tries to reserve or unlock it
- **Then** rejected (needs concrete window length)

### Max rental time
- Needs GWT: "Max Rental Time Exceeded" on the board (needs: limit, and whether the session is force-ended)

## Assumptions
- Boundaries and messages come from the 2026-11-05 discovery session; see the report for the expert's verbatim answers.

## Verification Metrics
- <to decide>

## Open Questions
- Name: the discovery map says Rental; the existing code package and reference CML say Rentals. Which one?
- Does exceeding max rental time end the session?
- Are the city's zones external, or defined by VeloCity?
- Can a bike still be rented between a locking failure and maintenance's decision?
