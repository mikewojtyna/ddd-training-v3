# Bounded Context Canvas: Rentals

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values. ddd-tactical-building-blocks then decides whether each one needs an aggregate, a domain service, a policy or a factory.

PRE-FILLED by discover-bounded-contexts (canvas only, no plan, no code) on 2026-11-05 from
docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md and
src/main/cml/VeloCity-Context-Map-Discovery.cml. Expert quotes are verbatim. Review before scaffolding.

UPDATED 2026-11-07 by docs/plans/rider-reserves-bike-at-station.md (approved): moved from rental/ to rentals/,
renamed to Rentals, reservation decisions added from docs/specs/rider-reserves-bike-at-station.md.
-->

## Name
Rentals

<!-- Matches @BoundedContext(name = "Rentals") in code and the reference CML. The discovery CML calls it `Rental`
     (decided 2026-11-07, Q1 of docs/plans/rider-reserves-bike-at-station.md). -->

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
| Rider app | Query | Available bikes at station | reserved and withdrawn bikes aren't listed |
| Rider Accounts | Query (answer) | Rider eligibility | asked before each rental |
| Fleet Maintenance | Command | Withdraw bike | Fleet Maintenance decides "out of service" and commands Rentals (flipped 2026-11-07, plan Q2) |
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
- **Reservation window**: "bike reserved has a reservation window which decides whether the bike is locked for this specific user." 15 minutes from the moment of reserving, including the end time: a reservation made at 10:00 still holds at 10:15 (product owner, 2026-11-07). Once it has passed, the reservation stops counting.
- **Reservation**: a borrower's hold on one specific bike at a station, made without being at the station.
- **Borrower**: the rider holding or renting a bike (code term; the spec says "rider").
- **Eligible borrower**: a borrower whose account Rider Accounts reports as activated.
- **Withdrawn bike**: a bike Fleet Maintenance has taken out of service; it isn't available, so it can't be reserved.
- **Rental session**: Starts at Rental Session Started, which "initiates the ride"; ends when the bike is returned (station or zone) or the session is ended.
- **Location check**: "User location is required to check whether user is allowed to rent a bike." Runs before every rental. It doesn't apply to reserving (product owner, 2026-11-07).
- **Station return / zone return**: Two endings of the same ride; "The same rules" except zone fees (Pricing).

## Business Decisions
<!-- Rules marked "Needs GWT" or with "?" need concrete values before scaffolding. -->
### R1 Reserve without being at the station (spec: rider-reserves-bike-at-station#E1.1, E1.2)
- **Given** bike B-17 is available at station Central and eligible borrower Anna is at home 2 km away
- **When** Anna reserves B-17 at 10:00
- **Then** B-17 is reserved for Anna until 10:15 (Bike Reserved)
- And (E1.2): **given** B-17 has just been rented by Ben, **when** Anna reserves B-17, **then** rejected

### R2 One active reservation per borrower (spec: …#E2.1)
- **Given** Anna holds a reservation for B-17 at Central until 10:15
- **When** Anna reserves B-22 at station Riverside at 10:05
- **Then** rejected, and B-22 stays available

### R3 A reserved bike is held for its borrower (spec: …#E3.1, E3.2)
- **Given** B-17 is reserved for Anna until 10:15
- **When** Ben reserves B-17 at 10:05 (E3.1), or rents B-17 at exactly 10:15 (E3.2)
- **Then** rejected
- After the window (plan D9, confirmed): **given** B-17 was reserved for Anna at 10:00, **when** Ben reserves B-17 at 10:16, **then** accepted

### R4 Reserved bikes aren't offered to others (spec: …#E4.1)
- **Given** B-17 and B-22 are at Central, B-17 is reserved for Anna and B-22 is available
- **When** Ben asks which bikes are available at Central
- **Then** he sees only B-22

### R5 Withdrawn bikes can't be reserved (spec: …#E5.1)
- **Given** B-31 at Central has been withdrawn (reported broken)
- **When** Anna reserves B-31
- **Then** rejected

### R6 Only eligible borrowers reserve (spec: …#E6.1)
- **Given** Carl hasn't finished onboarding (no verified payment method)
- **When** Carl reserves B-17
- **Then** rejected, and B-17 stays available

### R7 No reserving while riding (spec: …#E7.1)
- **Given** Anna rented B-05 at 09:40 and is still riding it
- **When** Anna reserves B-17 at Central at 10:00
- **Then** rejected, and B-17 stays available

### R8 A holder rents only the reserved bike (spec: …#E8.1, E8.2)
- **Given** Anna holds a reservation for B-17 at Central until 10:15 and B-22 is available
- **When** Anna rents B-22 at 10:05 (E8.1)
- **Then** rejected, and B-17 stays reserved for her
- And (E8.2): **when** Anna rents B-17 at 10:10, **then** the rental starts and her reservation ends (Bike Rented)

### Max rental time
- Needs GWT: "Max Rental Time Exceeded" on the board (needs: limit, and whether the session is force-ended)

## Assumptions
- Boundaries and messages come from the 2026-11-05 discovery session; see the report for the expert's verbatim answers.

## Verification Metrics
- <to decide>

## Open Questions
- ~~Name: Rental or Rentals?~~ Rentals (decided 2026-11-07).
- Does exceeding max rental time end the session?
- Are the city's zones external, or defined by VeloCity?
- Can a bike still be rented between a locking failure and maintenance's decision?
