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
| Rentals | Event | Unlock Failed (technician) | Bike stays broken and waits for a technician |
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
- **Then** B-17 is broken (Bike Reported Broken) (spec: fleet-operations-takes-broken-bike-out-of-circulation#E1.1)
- *Not returned:* Rentals reports B-22 withdrawn with reason *not returned* → B-22 is broken with fault source *not returned* (spec: fleet-operations-takes-broken-bike-out-of-circulation#E1.2, DE-21).

### A fleet-issue incident makes the bike broken and withdraws it
- **Given** B-17 isn't broken
- **When** Customer Support reports fleet issue I-1 for B-17
- **Then** B-17 is broken, linked to I-1 (Bike Reported Broken), and Rentals is asked to withdraw B-17 (spec: fleet-operations-takes-broken-bike-out-of-circulation#E2.1)

### A technician's fault report makes the bike broken and withdraws it
- **Given** B-22 is available at Central and isn't broken
- **When** technician Tom reports a fault on B-22 ("loose handlebar")
- **Then** B-22 is broken with fault source *technician* (Bike Reported Broken) and Rentals is asked to withdraw it (spec: technician-reports-fault#E1.1, DE-62)

### A duplicate fault report changes nothing
- **Given** B-17 is already broken
- **When** a second fault report for B-17 arrives from a technician
- **Then** nothing is recorded (a report from an incident only adds the incident link) (spec: technician-reports-fault#E3.1)
- *Incident on a broken bike:* B-17 broken since 10:40 (lock failed); fleet issue I-2 reported → I-2 linked, no second Bike Reported Broken, no second withdrawal request (spec: fleet-operations-takes-broken-bike-out-of-circulation#E3.1).

### Only a broken bike can be reserved by a technician
- **Given** B-22 isn't broken
- **When** technician Tom reserves B-22
- **Then** it is rejected; reserving broken B-17 asks Rentals for a service reservation for Tom (spec: technician-reserves-broken-bike#E2.1, #E1.1)
- *Already held:* B-17 reserved for Tom until 09:30; Tara reserves B-17 at 09:10 → rejected (Rentals' answer), B-17 stays reserved for Tom (spec: technician-reserves-broken-bike#E3.1).

### A technician's unlock starts maintenance
- **Given** B-17 is broken and reserved for Tom
- **When** Rentals reports B-17 unlocked by technician Tom
- **Then** maintenance on B-17 started (Maintenance Started) (spec: technician-starts-maintenance#E1.1)

### A failed unlock leaves the bike broken
- **Given** B-17 is broken and reserved for Tom until 09:30
- **When** Rentals reports Tom's unlock of B-17 failed at 09:20
- **Then** no maintenance starts, B-17 is still broken and any technician can reserve it (spec: technician-starts-maintenance#E4.1)

### A bike returns to circulation only after maintenance has started
- **Given** B-17 is broken and maintenance hasn't started
- **When** Tom returns B-17 to circulation
- **Then** it is rejected and B-17 stays broken (spec: technician-returns-fixed-bike-to-circulation#E2.1)

### Returning a bike under maintenance fixes it
- **Given** B-17 is under maintenance by Tom and linked to incident I-1
- **When** Tom returns B-17 to circulation at station Central
- **Then** B-17 is fixed (Bike Fixed), Rentals is asked to make B-17 available at Central and Customer Support to resolve I-1 (spec: technician-returns-fixed-bike-to-circulation#E1.1, #E3.1)
- *At a position, by another technician:* Tara returns B-22, under maintenance by Tom, at (52.2297, 21.0122) → fixed, Rentals asked to make it available at that position (spec: technician-returns-fixed-bike-to-circulation#E1.2, #E1.3, DE-91).
- *Not broken:* Tom returns available B-40 → rejected, nothing changes (spec: technician-returns-fixed-bike-to-circulation#E2.2).

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
