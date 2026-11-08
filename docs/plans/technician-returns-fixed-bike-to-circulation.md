---
name: technician-returns-fixed-bike-to-circulation
intent: technician-returns-fixed-bike-to-circulation
spec: technician-returns-fixed-bike-to-circulation
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A technician wants to return a fixed bike to circulation

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **FleetMaintenance**, which decides when a bike goes back into circulation (CML responsibility
"Returning bikes to circulation"): returning a bike under maintenance fixes it (R1, R2). It then asks **Rentals** to
make the bike available where the technician left it (Rentals' OHS *Make Bike Available*) and **Customer Support**
to resolve every linked fleet-issue incident (Customer Support's OHS *Resolve Fleet Issue*), which leaves an already
resolved incident untouched (R3). Neither upstream command is covered by another intent, so both are planned here.
No context map change: both calls go to the upstream's OHS (`Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`,
`CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance`). Order: canvases → Rentals → Customer Support → FleetMaintenance
→ acceptance tests.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 Returning a bike under maintenance fixes it and makes it available where the technician leaves it | FleetMaintenance (fixed, where); Rentals (available at the station or position) | FM decides "back in circulation" (CML; FM canvas "Returning a bike under maintenance fixes it", DE-60, DE-91); availability is Rentals' (Rentals canvas "A client makes a withdrawn bike available again") |
| R2 A bike returns to circulation only after maintenance has started | FleetMaintenance | reads FM's maintenance state (FM canvas "A bike returns to circulation only after maintenance has started") |
| R3 Fixing a bike resolves every linked fleet-issue incident that isn't resolved yet, whatever its state | FleetMaintenance (asks for every linked incident); Customer Support (resolves with "bike fixed", leaves a resolved incident as is) | FM holds the incident links (FM canvas "Incident link"); incident states are Customer Support's (CML "Incident handling", DE-55) |
| Command: Return Bike to Circulation | FleetMaintenance | FM can say no (R2) |
| Command: Make Bike Available (implied by Bike Made Available) | Rentals | Rentals canvas inbound "Fleet Maintenance / Make Bike Available" |
| Command: Resolve Fleet Issue (implied by Incident Resolved) | Customer Support | integration-contracts.md OHS; FM canvas outbound |
| Event: Bike Fixed | FleetMaintenance | the fact happens in FM; internal |
| Event: Bike Made Available | Rentals | the fact happens in Rentals; no listener outside |
| Event: Incident Resolved | Customer Support | the fact happens in Customer Support; no listener outside |

**Outcome:** several existing contexts (FleetMaintenance; Rentals and Customer Support as upstream OHS).

Hand-offs:
1. FM (D, ACL) → Rentals OHS *Make Bike Available* (`POST /rentals/bikes/{bikeId}/availability`).
2. FM (D, ACL) → Customer Support OHS *Resolve Fleet Issue* (`POST /customer-support/incidents/{incidentId}/fleet-resolution`), once per linked incident.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Return to circulation | FleetMaintenance | Return Bike to Circulation (= bike fixed, DE-91) | – |
| Return to circulation | Rentals | Make Bike Available (at a station or a position) | FM `rentalsctxacl` adapter |
| Drop zone position | Rentals | position (`latitude`/`longitude`), trusted as given (DE-22, DE-91) | FM `rentalsctxacl` adapter |
| Fleet-issue incident | FleetMaintenance | Incident link | – |
| Incident resolved "bike fixed" | Customer Support | Resolve Fleet Issue → resolution "bike fixed" | FM `customersupportctxacl` adapter |
| Technician | FleetMaintenance | Technician (any technician may return, DE-91) | REST adapter |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Return Bike to Circulation | FleetMaintenance | `ReturnBikeToCirculationUseCase` + record `ReturnBikeToCirculation(bikeId, technicianId, place)`; `POST /fleet-maintenance/bikes/{bikeId}/return-to-circulation` `{technicianId, stationId}` or `{technicianId, latitude, longitude}` (409 `MaintenanceNotStarted` for R2) |
| Make Bike Available | Rentals | `MakeBikeAvailableUseCase` + `MakeBikeAvailable(bikeId, stationId?, latitude?, longitude?)`; `POST /rentals/bikes/{bikeId}/availability`; FM port `CirculationReturns`, adapter `infra/adapters/secondary/rentalsctxacl/RentalsCirculationReturnsAdapter` |
| Resolve Fleet Issue | Customer Support | `ResolveFleetIssueUseCase` + `ResolveFleetIssue(incidentId, bikeId)`; `POST /customer-support/incidents/{incidentId}/fleet-resolution` `{bikeId}`; FM port `FleetIssueResolutions`, adapter `infra/adapters/secondary/customersupportctxacl/CustomerSupportFleetIssueResolutionsAdapter` |
| Event: Bike Fixed | FleetMaintenance | domain event `BikeFixed` (bike, technician, fixed at, linked incidents); no integration event |
| Event: Bike Made Available | Rentals | domain event `BikeMadeAvailable`; no integration event (no listener) |
| Event: Incident Resolved | Customer Support | domain event `IncidentResolved` (resolution "bike fixed"); no integration event (no listener) |

## Canvas changes

### FleetMaintenance (`docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`)

- **Business decisions, tag:** "Returning a bike under maintenance fixes it" → `(spec: technician-returns-fixed-bike-to-circulation#E1.1, #E3.1)`; "A bike returns to circulation only after maintenance has started" → `#E2.1`.
- **Business decisions, add** (to "Returning a bike under maintenance fixes it"): *At a position, by another technician:* Tara returns B-22, under maintenance by Tom, at (52.2297, 21.0122) → fixed, Rentals asked to make it available at that position (spec: technician-returns-fixed-bike-to-circulation#E1.2, #E1.3, DE-91). *Not broken:* Tom returns available B-40 → rejected, nothing changes (spec: technician-returns-fixed-bike-to-circulation#E2.2).

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, tag:** "A client makes a withdrawn bike available again" → `(spec: technician-returns-fixed-bike-to-circulation#E1.1)`; add the variant *at a position* (52.2297, 21.0122) → available at that position, no station (spec: technician-returns-fixed-bike-to-circulation#E1.2).

### CustomerSupport (`docs/bounded-contexts/velocity/customersupport/canvas.md`)

- **Business decisions, tag:** "A fixed bike resolves its fleet issue" → `(spec: technician-returns-fixed-bike-to-circulation#E3.1)`.
- **Business decisions, add** (to "A fixed bike resolves its fleet issue"): *Several and already resolved:* I-1 (accepted by Zoe) and I-2 (reported) about B-17; Fleet Maintenance reports B-17 fixed → both resolved with "bike fixed"; an incident Zoe already resolved with "brake adjusted on site" keeps her resolution and no second Incident Resolved (spec: technician-returns-fixed-bike-to-circulation#E3.1, #E3.2, DE-55).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md, CML `[D,ACL]` |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | trainer's standing instruction |
| D5 | Order of the hand-offs | FM checks R2, asks Rentals to make the bike available; only when Rentals accepts is Bike Fixed recorded; then each linked incident is resolved. A failed incident resolution is logged and doesn't undo the fix | a bike must never be "fixed" in FM while Rentals still withholds it; incidents are secondary to riders getting the bike |
| D6 | Already resolved incidents | FM asks for every linked incident; Customer Support's *Resolve Fleet Issue* is idempotent (resolved stays resolved, no second Incident Resolved) | the incident's state is Customer Support's; FM keeps no copy of it |
| D7 | Customer Support work in this plan | planned here (no Customer Support intent covers the automatic "bike fixed" resolution; `support-agent-resolves-incident` puts it out of scope) | same exception as the Rentals capabilities |

## Steps

### S1: Canvas updates for FleetMaintenance, Rentals and CustomerSupport
- **Status:** todo
- **Context:** FleetMaintenance, Rentals, CustomerSupport
- **Skill:** none (apply the "Canvas changes" diff exactly)
- **Prerequisite:** `technician-starts-maintenance` delivered.
- **Covers:** E1.2, E1.3, E2.2, E3.1, E3.2 (canvas only)
- **Done when:** the canvases carry the tags and additions above.

### S2: Rentals: make a withdrawn bike available again
- **Status:** todo
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1), via bounded-context-scaffolder update mode
- **Covers:** R1 (Rentals half), E1.1, E1.2
- **Done when:** domain tests: a withdrawn bike unlocked for a technician becomes locked, free and available at a station / at a position; rejected while reserved for a technician but not unlocked, or while a rider's session runs; `./mvnw test` green.

### S3: Rentals: Make Bike Available use case and REST (6b)
- **Status:** todo
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Bike Made Available
- **Done when:** `MakeBikeAvailableUseCase`, `POST /rentals/bikes/{bikeId}/availability` (404 unknown bike, 409 rule broken); REST-level test passes; `./mvnw test` green.

### S4: Customer Support: resolve a fleet issue because its bike is fixed
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1), via bounded-context-scaffolder update mode
- **Prerequisite:** the incident lifecycle from `rider-reports-incident`, `support-agent-decides-on-incident` and `support-agent-resolves-incident`.
- **Covers:** R3 (Customer Support half), E3.1, E3.2
- **Done when:** domain tests: reported and accepted fleet issues are resolved with "bike fixed"; a resolved incident keeps its resolution and raises nothing; `./mvnw test` green.

### S5: Customer Support: Resolve Fleet Issue use case and REST (6b)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Incident Resolved
- **Done when:** `ResolveFleetIssueUseCase`, `POST /customer-support/incidents/{incidentId}/fleet-resolution` (404 unknown incident); REST-level test passes; `./mvnw test` green.

### S6: FleetMaintenance: ports, adapters, records (6a)
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6a, update mode)
- **Covers:** none
- **Done when:** record `ReturnBikeToCirculation`; ports `CirculationReturns` (`rentalsctxacl`) and `FleetIssueResolutions` (`customersupportctxacl`) with adapters; architecture test green; `./mvnw test` green.

### S7: FleetMaintenance: returning a bike to circulation
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R2, R3, E1.1, E1.2, E1.3, E2.1, E2.2, E3.1
- **Done when:** domain tests: a bike under maintenance returned at a station / position by any technician is fixed (Bike Fixed with its linked incidents); a bike not under maintenance or not broken is rejected; `./mvnw test` green.

### S8: FleetMaintenance: use case and REST (6b)
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Return Bike to Circulation, Bike Fixed
- **Done when:** `ReturnBikeToCirculationUseCase` with the hand-off order of D5, `POST /fleet-maintenance/bikes/{bikeId}/return-to-circulation`, `GET /fleet-maintenance/bikes/{bikeId}` reporting `IN_CIRCULATION`; `./mvnw test` green.

### S9: Acceptance tests for the spec examples
- **Status:** todo
- **Context:** FleetMaintenance, Rentals, CustomerSupport
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `TechnicianReturnsFixedBikeToCirculationSpecTest` (`@VelocitySpringTest`, real beans, `MutableClock`; set-up through the REST APIs: fleet issues via Customer Support, reservation and unlock via Fleet Maintenance) has one test per example; the FM → Rentals and FM → Customer Support hand-offs are asserted on `GET /rentals/bikes/{bikeId}` and `GET /customer-support/incidents/{incidentId}` with Awaitility; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `TechnicianReturnsFixedBikeToCirculationSpecTest#e1_1_return_at_station_fixes_bike_and_makes_it_available` |
| E1.2 | `TechnicianReturnsFixedBikeToCirculationSpecTest#e1_2_return_in_drop_zone_makes_bike_available_at_position` |
| E1.3 | `TechnicianReturnsFixedBikeToCirculationSpecTest#e1_3_any_technician_may_return_bike` |
| E2.1 | `TechnicianReturnsFixedBikeToCirculationSpecTest#e2_1_return_before_maintenance_started_is_rejected` |
| E2.2 | `TechnicianReturnsFixedBikeToCirculationSpecTest#e2_2_return_of_bike_not_broken_is_rejected` |
| E3.1 | `TechnicianReturnsFixedBikeToCirculationSpecTest#e3_1_fix_resolves_every_open_linked_incident` |
| E3.2 | `TechnicianReturnsFixedBikeToCirculationSpecTest#e3_2_already_resolved_incident_keeps_its_resolution` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1, E1.2 | S2, S3, S7, S8, S9 | |
| R1 / E1.3 | S1, S7, S8, S9 | |
| R2 / E2.1, E2.2 | S1, S7, S8, S9 | |
| R3 / E3.1 | S4, S5, S6, S7, S8, S9 | |
| R3 / E3.2 | S4, S5, S9 | |

## Risks and open questions

- D5 means a Customer Support outage leaves incidents open after the fix; acceptable for the training, recorded for review.
- Retiring an unrepairable bike (DE-61) stays out of scope.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (FM decides the fix, Rentals the availability, Customer Support the incident state; both calls go to upstream OHS in the CML direction; every example covered; ≤3 rules per tactical step; no building blocks decided).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
