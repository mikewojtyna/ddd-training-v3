# Bounded Context Canvas: FleetMaintenance

<!--
Rewritten by Claude on 2026-11-08 from docs/domains/velocity/velocity-vision.md ("Broken Bikes & Maintenance"),
docs/domains/velocity/velocity-context-map.md, the CML and the earlier canvas of 2026-11-03, during an autonomous
run. Values from role-played domain-expert answers (DE-n), see docs/bounded-contexts/velocity/domain-expert-answers.md.
-->

## Name
FleetMaintenance

## Purpose
Keeps the fleet healthy: knows every broken bike, whatever reported it, lets technicians pick broken bikes up and
repair them, and decides when a repaired bike goes back into circulation. Maintenance is never billed.

## Strategic Classification
- Domain: supporting
- Business model: cost reduction
- Evolution: custom built

## Domain Roles
- Enforcer: enables Rentals to offer only working bikes.
- Execution Model: runs each broken bike through maintenance.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Rentals | Event | Bike Withdrawn (fault reported at return, lock failed, fault signalled, not returned) | Rentals withdrew it itself; we register it broken |
| Rentals | Event | Bike Unlocked (by a technician) | We start maintenance |
| Rentals | Event | Reservation Expired / Unlock Failed (technician) | Bike stays broken and waits for a technician |
| Customer Support | Event | Fleet Issue Reported | Confirmed (DE-52); we register it broken and ask Rentals to withdraw it |
| Technician app | Command | Report Fault | DE-62 |
| Technician app | Command | Reserve Broken Bike | Via Rentals' service reservation |
| Technician app | Command | Unlock Broken Bike | Via Rentals' service unlock |
| Technician app | Command | Return Bike to Circulation (station or position) | Means fixed and back (DE-60) |
| Technician app | Query | Broken Bikes | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Withdraw Bike | Command | Rentals | yes (Rentals speaks *available / withdrawn*, never *broken*; the map now says `[D,ACL]`) |
| Reserve Bike / Unlock Bike (for service, technician) | Command | Rentals | yes |
| Make Bike Available | Command | Rentals | yes |
| Resolve Fleet Issue (incident, bike fixed) | Command | Customer Support | yes (Customer Support's OHS) |
| Bike Reported Broken, Maintenance Started, Bike Fixed | Event | none outside Fleet Maintenance yet | n/a |

## Ubiquitous Language
- **Broken bike**: a bike reported faulty; riders can't rent it until it's fixed.
- **Fault report**: a report that a bike is broken: from Rentals (it withdrew the bike), from a fleet-issue incident, or from a technician.
- **Incident link**: a fleet-issue incident about the broken bike, resolved when the bike is fixed.
- **Technician**: a VeloCity employee who reserves and unlocks broken bikes to repair them.
- **Maintenance**: the work after a technician's unlock; never billed.
- **Maintenance started**: the moment a technician's unlock of a broken bike is reported.
- **Bike fixed / back in circulation**: the technician returned the bike to circulation after maintenance; there is no separate "mark fixed" step.

## Business Decisions

### A bike withdrawn by Rentals is registered broken
- **Given** bike B-17 isn't broken
- **When** Rentals reports B-17 withdrawn with reason *lock failed*
- **Then** B-17 is broken (Bike Reported Broken)

### A fleet-issue incident makes the bike broken and withdraws it
- **Given** B-17 isn't broken
- **When** Customer Support reports fleet issue I-1 for B-17
- **Then** B-17 is broken, linked to I-1 (Bike Reported Broken), and Rentals is asked to withdraw B-17

### A duplicate fault report changes nothing
- **Given** B-17 is already broken
- **When** a second fault report for B-17 arrives from a technician
- **Then** nothing is recorded (a report from an incident only adds the incident link)

### Only a broken bike can be reserved by a technician
- **Given** B-22 isn't broken
- **When** technician Tom reserves B-22
- **Then** it is rejected; reserving broken B-17 asks Rentals for a service reservation for Tom

### A technician's unlock starts maintenance
- **Given** B-17 is broken and reserved for Tom
- **When** Rentals reports B-17 unlocked by technician Tom
- **Then** maintenance on B-17 started (Maintenance Started)

### An expired reservation leaves the bike broken
- **Given** B-17 is broken and reserved for Tom
- **When** Rentals reports Tom's reservation of B-17 expired
- **Then** B-17 is still broken and can be reserved by any technician

### A bike returns to circulation only after maintenance has started
- **Given** B-17 is broken and maintenance hasn't started
- **When** Tom returns B-17 to circulation
- **Then** it is rejected and B-17 stays broken

### Returning a bike under maintenance fixes it
- **Given** B-17 is under maintenance by Tom and linked to incident I-1
- **When** Tom returns B-17 to circulation at station Central
- **Then** B-17 is fixed (Bike Fixed), Rentals is asked to make B-17 available at Central and Customer Support to resolve I-1

## Assumptions
- 🎭 DE-52: fleet-issue incidents reach Fleet Maintenance (the reference map's unconfirmed link is now confirmed).
- 🎭 DE-21: a bike not returned after the maximum rental time is treated as broken.
- Rentals owns reserve/unlock; Fleet Maintenance never locks or unlocks bikes itself.
- 🎭 DE-91: any technician may return a bike under maintenance, at the station or position they name.
- 🎭 DE-92: a technician may hold several reservations; a fault report on a bike in a ride is accepted and the ride goes on.
- A withdrawal that Fleet Maintenance asked for itself (reason *requested*) is not a new fault report.

## Verification Metrics
- Time from "reported broken" to "back in circulation".
- Share of bikes reported broken again within 7 days of returning to circulation.

## Open Questions
- Can a bike turn out to be unrepairable (retired from the fleet)? (DE-61: out of scope.)
- Is there a repair priority (e.g. bikes at busy stations first)?
