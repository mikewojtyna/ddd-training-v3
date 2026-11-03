# Bounded Context Canvas: FleetMaintenance

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become aggregates and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude from velocity/velocity-vision.md, velocity/velocity-context-map.md and
src/main/cml/VeloCity-Context-Map.cml, then completed with the user on 2026-11-03.
-->

## Name
FleetMaintenance

<!-- Matches `BoundedContext FleetMaintenance` in the CML. -->

## Purpose
Keeps the fleet healthy: makes sure a broken bike is fixed as quickly as possible and decides
when it goes back into circulation.

## Strategic Classification
- Domain: supporting
- Business model: cost reduction
- Evolution: custom built

## Domain Roles
<!-- Pick traits from docs/model-traits-worksheet.md -->
- Enforcer: enables Rentals to offer only working bikes

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Rentals | Event | Bike Withdrawn (fault detected / lock failed) | Rentals withdraws a faulty bike itself; we learn it's broken |
| Rentals | Event | Bike Unlocked (by a technician) | We react with *Maintenance Started*; never a rental session |
| Rentals | Event | Reservation Expired / Unlock Failed (technician) | Bike stays broken and waits for another technician |
| Customer Support | Event | Fleet Issue Incident Reported | Default assumption in the context map, NOT confirmed |
| Technician app | Command | Report Fault | A technician reports a bike broken themselves, e.g. during a routine check |
| Technician app | Command | Return Bike to Circulation | Means "fixed and back": publishes *Bike Fixed* and makes the bike available in Rentals |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Withdraw Bike | Command | Rentals | yes (Rentals speaks *available / withdrawn*, never *broken*) |
| Make Bike Available (back in circulation) | Command | Rentals | yes |
| Reserve Bike / Unlock Bike (for a technician) | Command | Rentals | yes |
| Maintenance Started | Event | none (internal domain event) | n/a for events |
| Bike Fixed | Event | none (internal domain event) | n/a for events |
| Notify Bike Fixed | Command | Customer Support (so it can resolve the fleet-issue incident) | yes. Customer Support stays upstream; Fleet Maintenance is downstream and calls its command |

## Ubiquitous Language
- **Bike**: a physical vehicle in the VeloCity fleet.
- **Broken**: a bike that has been reported faulty; riders can't rent it until it's fixed.
- **Fault Report**: a report that a bike is broken, from a rider at return, from a support incident, from the bike itself, or from a technician.
- **Technician**: a VeloCity employee who reserves and unlocks bikes in order to repair them.
- **Maintenance**: the work a technician does on a broken bike after unlocking it; never billed.
- **Maintenance Started**: the moment a technician unlocks a broken bike.
- **Bike Fixed**: the moment a technician returns a bike to circulation after maintenance. There is no separate "mark fixed" step: returning it means it is fixed.
- **Back in Circulation**: the bike is at a station or in a drop zone and available to riders again.

## Business Decisions
<!-- One rule per block. Concrete values, please.
     The ones below are derived from the vision. Confirm, sharpen or delete them, and add your own.
     Rules about time or limits need concrete numbers to become code. -->

### A bike returns to circulation only after maintenance has started
- **Given** bike B-17 is broken and maintenance on it has not started
- **When** a technician returns B-17 to circulation
- **Then** the request is rejected and B-17 stays withdrawn

### Returning a bike under maintenance fixes it
- **Given** bike B-17 is broken and maintenance on it has started
- **When** a technician returns B-17 to circulation
- **Then** *Bike Fixed* is recorded for B-17 and Rentals is asked to make B-17 available

### Maintenance starts only on a broken bike
- **Given** bike B-17 is not broken
- **When** a technician's unlock of B-17 is reported
- **Then** the unlock is ignored: nothing is recorded and no *Maintenance Started* is published

### A broken bike waits after an expired reservation
- **Given** bike B-17 is broken and reserved by technician T-1
- **When** the reservation expires before T-1 unlocks it
- **Then** B-17 is still broken, is not offered to riders, and can be reserved by any technician

### A duplicate fault report is ignored
- **Given** bike B-17 is already broken
- **When** a second fault report for B-17 arrives
- **Then** nothing changes and nothing is recorded

## Assumptions
- Fleet-issue incidents from Customer Support reach Fleet Maintenance (unconfirmed in the context map).
- Faults signalled by the bike itself are received by Rentals, which owns locks and stations (unconfirmed).
- Rentals owns reserve/unlock; Fleet Maintenance never locks or unlocks bikes itself.

## Verification Metrics
- Time from "reported broken" to "back in circulation".
- Share of bikes reported broken again within 7 days of returning to circulation.

## Open Questions
- Can a bike turn out to be unrepairable (retired from the fleet)? Who decides?
- Is there a repair priority (e.g. bikes at busy stations first)?
