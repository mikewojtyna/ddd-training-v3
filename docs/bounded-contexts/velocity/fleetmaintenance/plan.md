# FleetMaintenance bounded context: implementation plan

- **System / package**: `velocity.fleetmaintenance`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`
- **Last updated**: 2026-11-03
- **Status**: implemented

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-03: draft written, awaiting review. Questions for the reviewer are marked **Q1–Q8** below.
- 2026-11-03, review round 1: the user said that Fleet Maintenance must not publish *Bike Fixed* to Customer Support. Fleet Maintenance stays downstream and calls a command on Customer Support instead. Changed: canvas outbound table (new *Notify Bike Fixed* command, *Bike Fixed* event now internal), D9, outbound mapping, new rule R6, files (no `api/events`, no `IntegrationEventsPublisher`; new port `FixNotifications` + `customersupportctxacl` adapter + policy), differences from the map. Q8 is resolved. Q1–Q7 still open.
- 2026-11-03: approved by the user as written, with Q1–Q7 still open. The suggested defaults apply: Q3 not modelled (no `TakeBrokenBikeOutOfCirculationPolicy`), Q5 and Q6 ignored, Q7 any technician; U1 and U2 implemented as derived rules.

## Canvas summary
Fleet Maintenance keeps the fleet healthy: a broken bike gets fixed as quickly as possible, and this context decides when it goes back into circulation. It is a **supporting** domain with a **cost reduction** business model, **custom built**. Its role is **Enforcer**: it makes sure Rentals offers only working bikes, by telling Rentals to withdraw a broken bike and to make it available again only once it is fixed.

## Decisions
| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | System / package | `velocity` / `fleetmaintenance` | CML `BoundedContext FleetMaintenance`; `@BoundedContext(name = "FleetMaintenance")` matches the CML and the canvas |
| D2 | Modules | 1 module = 1 context | default |
| D3 | ACL towards Rentals (outbound commands) | yes: port `Circulation` in our language, adapter in `infra/adapters/secondary/rentalsctxacl` | canvas says ACL; Rentals speaks *available / withdrawn*, we speak *broken / back in circulation* |
| D4 | ACL for inbound events from Rentals and Customer Support | yes: listeners in `infra/adapters/primary/rentalsctxacl` and `.../customersupportctxacl` | default. **Not generated now**: Rentals has no `api/events` yet and Customer Support doesn't exist in code (see follow-ups). Only our use cases and our own command records are created |
| D5 | Aggregate | one aggregate root **`Bike`** in `domain/bike`, one instance per bike, with its own `BikeId` record | every rule's *Given* is about the condition of one bike (broken, maintenance started), so one bike is the smallest consistency boundary. `BikeId` is our own type, not Rentals' `BikeId`, because the boundary rule forbids using Rentals' domain types |
| D6 | Reservations | Fleet Maintenance **does not track reservations**. Rentals owns reserve / unlock (canvas assumption, and the reference map) | Rule R4 only needs "the bike stays broken and any technician can start maintenance". Tracking who reserved a bike would duplicate Rentals' state. Consequence: the inbound row *Reservation Expired / Unlock Failed* needs no handling here, see **Q1** |
| D7 | Calls to Rentals | made by **policies** reacting to domain events (`BikeFixed` → put back into circulation), not by the use case or the aggregate | "whenever X then Y" belongs in a `DomainPolicy`; the aggregate stays free of outbound calls, the same way deposit's `DepositInitiatedPolicy` works |
| D8 | Unknown bike | a bike Fleet Maintenance has never heard of is **in circulation** (not broken). The repository returns `Optional`, and use cases fall back to `Bike.inCirculation(id)` | Fleet Maintenance learns about a bike from its first fault report; it doesn't need a copy of the fleet register |
| D9 | Published events | **none**. `BikeFixed` and `MaintenanceStarted` are internal domain events. Customer Support learns about a fix through a **command** Fleet Maintenance calls: port `FixNotifications` in our language, adapter in `infra/adapters/secondary/customersupportctxacl` (ACL). No `api/events` package | review round 1: Customer Support stays upstream of Fleet Maintenance in both relationships, so there's no cycle |
| D10 | REST for the technician app | implemented (not a stub) in `infra/adapters/primary/TechnicianController` | the canvas describes both technician commands |
| D11 | Boundary exceptions | none | no request to break the `api`-only rule |
| D12 | ArchUnit | add `com.tngtech.archunit:archunit-junit5:1.5.1` (test scope) to `pom.xml` | not on the classpath yet; older versions can't read JDK 27 class files |
| D13 | Outbound *Reserve Bike / Unlock Bike (for a technician)* | **not modelled** | nothing in the canvas triggers it, and it contradicts the assumption "Fleet Maintenance never locks or unlocks bikes itself". See **Q2** |

## Mapping: canvas → code
Paths relative to `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/`.

### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rentals | Event | Bike Withdrawn (fault detected / lock failed) | `api/usecases/ReportFaultUseCase` + record `ReportFault(String bikeId)`. Listener in `infra/adapters/primary/rentalsctxacl`: **follow-up** (Rentals publishes no events yet) |
| Rentals | Event | Bike Unlocked (by a technician) | `api/usecases/StartMaintenanceUseCase` + record `BikeUnlockedByTechnician(String bikeId, String technicianId)`. Listener: **follow-up** |
| Rentals | Event | Reservation Expired / Unlock Failed (technician) | **no code** (D6, **Q1**) |
| Customer Support | Event | Fleet Issue Incident Reported (unconfirmed) | same `ReportFaultUseCase`. Listener in `infra/adapters/primary/customersupportctxacl`: **follow-up** (context not in code) |
| Technician app | Command | Report Fault | `ReportFaultUseCase`, REST `POST /fleet-maintenance/bikes/{bikeId}/fault-reports` in `TechnicianController` |
| Technician app | Command | Return Bike to Circulation | `api/usecases/ReturnBikeToCirculationUseCase` + record `ReturnBikeToCirculation(String bikeId, String technicianId)`, REST `POST /fleet-maintenance/bikes/{bikeId}/return-to-circulation` |

Use-case flow (as in deposit): load the `Bike` (or `Bike.inCirculation(id)`, D8), call its behaviour, save it, `DomainEventPublisher.publish(events)`.

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Withdraw Bike | Command | Rentals | `domain/bike/Circulation#takeOut(BikeId)` | `infra/adapters/secondary/rentalsctxacl/RentalsCirculationAdapter`, **stub** (Rentals has no withdraw in its `api`). Called by policy `TakeBrokenBikeOutOfCirculationPolicy` **only if Q3 is confirmed** |
| Make Bike Available | Command | Rentals | `Circulation#putBack(BikeId)` | same adapter, **stub**. Called by policy `ReturnFixedBikeToCirculationPolicy` |
| Reserve Bike / Unlock Bike | Command | Rentals | none | none (D13, **Q2**) |
| Maintenance Started | Event | none | domain event `BikeMaintenanceEvent.MaintenanceStarted` | internal only |
| Bike Fixed | Event | none | domain event `BikeMaintenanceEvent.BikeFixed` | internal only |
| Notify Bike Fixed | Command | Customer Support | `domain/bike/FixNotifications#bikeFixed(BikeId)` | `infra/adapters/secondary/customersupportctxacl/CustomerSupportFixNotificationsAdapter`, **stub** (Customer Support isn't in code). Called by policy `NotifySupportOfFixedBikePolicy` |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Bike | `domain/bike/Bike`, `BikeId` | aggregate root, identity (VO record) |
| Broken | private state of `Bike`; event `BikeReportedBroken` | aggregate state + domain event |
| Fault Report | command record `ReportFault`; `Bike#reportFault()` | command / behaviour |
| Technician | `domain/bike/TechnicianId` | VO record |
| Maintenance | private state of `Bike` | aggregate state (javadoc) |
| Maintenance Started | `BikeMaintenanceEvent.MaintenanceStarted(BikeId, TechnicianId)` | domain event |
| Bike Fixed | `BikeMaintenanceEvent.BikeFixed(BikeId, TechnicianId)`; `FixNotifications#bikeFixed` | domain event + outbound port |
| Back in Circulation | `Bike#returnToCirculation(TechnicianId)`; `Circulation#putBack` | behaviour / port |

Rejection: `domain/bike/MaintenanceNotStarted` (exception). Events: sealed `BikeMaintenanceEvent extends DomainEvent`.

### Business rules → aggregates
All rules live in the `Bike` aggregate (D5). Test class: `src/test/java/.../velocity/fleetmaintenance/domain/bike/BikeTest`.

| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| **R1** Given B-17 broken and maintenance not started, when a technician returns B-17 to circulation, then rejected and B-17 stays withdrawn | a bike can only be returned to circulation from maintenance | `Bike#returnToCirculation` throws `MaintenanceNotStarted`, state unchanged, no event | `BikeTest#returnWithoutMaintenanceIsRejected` |
| **R2** Given B-17 broken and maintenance started, when a technician returns B-17 to circulation, then *Bike Fixed* is recorded and Rentals is asked to make B-17 available | returning from maintenance = fixed | `Bike#returnToCirculation` returns `BikeFixed`; `ReturnFixedBikeToCirculationPolicy` calls `Circulation#putBack` | `BikeTest#returningBikeUnderMaintenanceFixesIt`, `ReturnFixedBikeToCirculationPolicyTest#fixedBikeIsPutBackIntoCirculation` |
| **R3** Given B-17 not broken, when a technician's unlock of B-17 is reported, then ignored: nothing recorded, no *Maintenance Started* | maintenance starts only on a broken bike | `Bike#startMaintenance` returns no events | `BikeTest#technicianUnlockOfBikeThatIsNotBrokenIsIgnored` |
| **R4** Given B-17 broken and reserved by T-1, when the reservation expires before T-1 unlocks it, then B-17 is still broken, not offered to riders, and can be reserved by any technician | the bike isn't bound to one technician; only returning from maintenance puts it back | modelled for the Fleet Maintenance side only (D6): after T-1's expired reservation, T-2's unlock starts maintenance, and no `BikeFixed` is recorded. "Can be reserved" itself is Rentals' state | `BikeTest#anyTechnicianCanStartMaintenanceAfterExpiredReservation` |
| **R5** Given B-17 already broken, when a second fault report arrives, then nothing changes and nothing is recorded | a fault report on a broken bike is idempotent | `Bike#reportFault` returns no events | `BikeTest#duplicateFaultReportIsIgnored` |
| **U1** (from the definition of *Broken*) Given B-17 in circulation, when a fault is reported, then B-17 is broken (*Bike Reported Broken* recorded) | — | `Bike#reportFault` returns `BikeReportedBroken` | `BikeTest#faultReportMakesBikeBroken` |
| **U2** (from the definition of *Maintenance Started*) Given B-17 broken, when technician T-1's unlock of B-17 is reported, then *Maintenance Started* by T-1 is recorded | — | `Bike#startMaintenance` returns `MaintenanceStarted` | `BikeTest#technicianUnlockOfBrokenBikeStartsMaintenance` |
| **R6** (from review round 1) Given B-17 under maintenance, when a technician returns B-17 to circulation, then Customer Support is told B-17 is fixed | every fix reaches Customer Support | `NotifySupportOfFixedBikePolicy` calls `FixNotifications#bikeFixed` on `BikeFixed` | `NotifySupportOfFixedBikePolicyTest#supportIsToldBikeIsFixed` |

U1 and U2 aren't written as rules in the canvas, but every rule's *Given* depends on them and they follow directly from the ubiquitous-language definitions. **Q4**: confirm them, or add them to the canvas as rules.

### Rules awaiting GWT
Not modelled until you confirm. Each comes with a suggested GWT.

- **Q3 – withdrawing a broken bike in Rentals.** The outbound row *Withdraw Bike* has no rule saying when it's sent. Suggested: *Given B-17 in circulation, when a fault is reported (from any source), then Rentals is asked to withdraw B-17.* This also sends it when Rentals itself reported the fault (it has already withdrawn the bike), so Rentals' withdraw must be idempotent. Alternative: send it only for reports from Customer Support and technicians. If confirmed: policy `TakeBrokenBikeOutOfCirculationPolicy` + `TakeBrokenBikeOutOfCirculationPolicyTest`.
- **Q5 – fault report during maintenance.** Suggested: *Given maintenance on B-17 has started, when a fault report for B-17 arrives, then nothing changes and nothing is recorded* (the bike is still broken, so R5 applies). Until confirmed, the code will behave this way only because a bike under maintenance is still broken — tell me if that's wrong.
- **Q6 – second technician unlock during maintenance.** *Given maintenance on B-17 was started by T-1, when T-2's unlock of B-17 is reported, then* ? (ignored / maintenance handed over to T-2 / rejected).
- **Q7 – who may return the bike.** *Given maintenance on B-17 was started by T-1, when T-2 returns B-17 to circulation, then* ? (allowed / rejected). Until answered, any technician may (no check is coded).

### Open design questions
- **Q1 – Reservation Expired / Unlock Failed.** With D6 these two inbound events change nothing in Fleet Maintenance. I suggest removing the row from the canvas (and from the CML comment). Alternative: keep it and track technician reservations here, which duplicates Rentals.
- **Q2 – Reserve / Unlock for a technician.** Nothing in the canvas triggers this outbound command, and the canvas assumption says Fleet Maintenance never locks or unlocks bikes. I suggest removing the row: the technician app reserves and unlocks through Rentals directly, as the reference map says ("Reserve / unlock is a generic Rentals capability"). Alternative: add an inbound technician command *Reserve Bike for Repair* that Fleet Maintenance forwards to Rentals.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Fleet Maintenance calls a *Notify Bike Fixed* command on Customer Support | `CustomerSupport [U]->[D] FleetMaintenance` carries only the fleet-issue incident (unconfirmed) | No, but it keeps the same direction: Customer Support is upstream for both the incident feed and the command, so there's no cycle (Q8 resolved in review round 1). The CML relationship comment would gain "notify bike fixed", and Customer Support may need an `OHS` marker |
| ACL towards Rentals | `Rentals [U,OHS,PL]->[D,CF] FleetMaintenance`: Fleet Maintenance is **Conformist** | No, but compatible: an ACL on top of Rentals' OHS/PL is a stricter choice than CF. The CML would change `CF` to `ACL` |
| Rentals reports faults as *Bike Withdrawn* events | comment: "fault detected, lock failed; withdraw / make available; reserve / unlock" | Same relationship, same direction. The *reserve / unlock* part is in question (Q2) |
| Technician app as a collaborator | not a context, not in the map | n/a (a UI, not a bounded context) |

## Files
```
pom.xml                                                     + archunit-junit5 1.5.1 (test)
src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/
  package-info.java                                         @BoundedContext(name = "FleetMaintenance") + @Module, canvas javadoc
  api/usecases/
    ReportFault.java                                        record
    ReportFaultUseCase.java                                 @PrimaryPort
    BikeUnlockedByTechnician.java                           record
    StartMaintenanceUseCase.java                            @PrimaryPort
    ReturnBikeToCirculation.java                            record
    ReturnBikeToCirculationUseCase.java                     @PrimaryPort
  domain/bike/
    Bike.java                                               @AggregateRoot
    BikeId.java                                             @ValueObject record
    TechnicianId.java                                       @ValueObject record
    BikeMaintenanceEvent.java                               sealed: BikeReportedBroken, MaintenanceStarted, BikeFixed
    MaintenanceNotStarted.java                              exception
    BikeRepository.java                                     @Repository @SecondaryPort
    Circulation.java                                        @SecondaryPort (takeOut, putBack)
    FixNotifications.java                                   @SecondaryPort (bikeFixed)
    ReturnFixedBikeToCirculationPolicy.java                 DomainPolicy
    NotifySupportOfFixedBikePolicy.java                     DomainPolicy
    (TakeBrokenBikeOutOfCirculationPolicy.java              only if Q3 confirmed)
  infra/adapters/primary/
    TechnicianController.java                               @PrimaryAdapter, REST
    ReturnToCirculationRequest.java                         record, REST request body
  infra/adapters/secondary/
    InMemoryBikeRepository.java                             @SecondaryAdapter
  infra/adapters/secondary/rentalsctxacl/
    package-info.java                                       @AntiCorruptionLayer
    RentalsCirculationAdapter.java                          @SecondaryAdapter, stub
  infra/adapters/secondary/customersupportctxacl/
    package-info.java                                       @AntiCorruptionLayer
    CustomerSupportFixNotificationsAdapter.java             @SecondaryAdapter, stub
  infra/event/
    FleetMaintenanceLocalEventsHandler.java                 @EventListener → policies
  infra/config/
    FleetMaintenanceContextConfig.java                      @Configuration, beans prefixed "fleetMaintenance"
src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/
  FleetMaintenanceArchitectureTest.java                     incoming + outgoing api-only rules
  domain/bike/BikeTest.java                                 R1–R5, U1, U2
  domain/bike/ReturnFixedBikeToCirculationPolicyTest.java   R2 (Rentals is asked)
  domain/bike/NotifySupportOfFixedBikePolicyTest.java       R6 (Customer Support is told)
```

## Stubs and follow-ups
- `RentalsCirculationAdapter` is a stub (`UnsupportedOperationException("implement this")`): Rentals has no withdraw / make-available use case in its `api` yet. Until then, returning a fixed bike will fail at the adapter when run through Spring. The domain tests use a fake `Circulation`.
- Inbound listeners not generated: `rentalsctxacl` (Bike Withdrawn, technician Bike Unlocked) — Rentals has no `api/events`; `customersupportctxacl` (Fleet Issue Incident) — Customer Support isn't in code.
- `CustomerSupportFixNotificationsAdapter` is a stub: Customer Support isn't in code, so its command's name and shape are unknown. The adapter's comment quotes the canvas message *Notify Bike Fixed*.
- CML / `docs/domains/velocity/velocity-context-map.md`: the CustomerSupport → FleetMaintenance relationship comment would gain "notify bike fixed" (Customer Support possibly `OHS`), possibly `CF` → `ACL` towards Rentals, and the Rentals relationship comment updated after Q1/Q2. Not changed unless you ask.
- Canvas open questions, copied into the javadoc: unrepairable bikes (retirement), repair priority.
- Customer Support may need an incident id with *Notify Bike Fixed* to close the right incident, and Fleet Maintenance would have to keep it from the fault report. The canvas doesn't say. Left as a question in the javadoc.

## Changelog
### 2026-11-03: scaffolded
- Added: everything under "Files". `Bike` aggregate (R1–R5, U1, U2), policies `ReturnFixedBikeToCirculationPolicy` (R2) and `NotifySupportOfFixedBikePolicy` (R6), three use cases, `TechnicianController` (`MaintenanceNotStarted` → HTTP 409), in-memory repository, stub ACL adapters towards Rentals and Customer Support, `FleetMaintenanceContextConfig`, `FleetMaintenanceArchitectureTest`, ArchUnit 1.5.1 in `pom.xml`.
- Not in the file list before: `ReturnToCirculationRequest` (REST request body record).
- Not added: `TakeBrokenBikeOutOfCirculationPolicy` (Q3 unconfirmed). `Circulation#takeOut` exists but is not called.
- Verified: `./mvnw test`, 16 tests green, including the Spring context.
- Triggered by: plan approved on 2026-11-03.

### 2026-11-03: plan drafted
- Added: everything above (not implemented yet).
- Triggered by: new canvas `canvas.md`, completed with the user on 2026-11-03.
