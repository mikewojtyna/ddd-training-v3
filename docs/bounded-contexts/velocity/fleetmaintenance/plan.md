# FleetMaintenance bounded context: implementation plan

- **System / package**: `velocity.fleetmaintenance`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`
- **Last updated**: 2026-11-08
- **Status**: implemented

## Review notes
- 2026-11-08: approved via docs/plans/fleet-operations-takes-broken-bike-out-of-circulation.md,
  docs/plans/technician-reports-fault.md, docs/plans/technician-reserves-broken-bike.md,
  docs/plans/technician-starts-maintenance.md and docs/plans/technician-returns-fixed-bike-to-circulation.md
  (FleetMaintenance steps), plus docs/plans/rider-reports-incident.md S6 (autonomous run, 2026-11-08).

## Canvas summary
Supporting, cost-reduction, custom-built context. Enforcer (Rentals offers only working bikes) and Execution Model
(each broken bike runs through maintenance): it knows every broken bike, lets technicians reserve, unlock and repair
them through Rentals, and decides when a repaired bike goes back into circulation.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards Rentals | yes: `infra/adapters/secondary/rentalsctxacl` (REST) and `infra/adapters/primary/rentalsctxacl` (events) | CML `Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`; Rentals says *withdrawn* and *holder TECHNICIAN*, never *broken* |
| ACL towards Customer Support | yes: `infra/adapters/secondary/customersupportctxacl`, `infra/adapters/primary/customersupportctxacl` | CML `CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance` |
| Boundary exceptions | none | |
| Domain layer | `ddd-tactical-building-blocks` rules (one aggregate `Bike`) | every rule reads the one bike's condition |
| TDD checkpoint loop | off (trainer's explicit autonomy request, 2026-11-08; D1 of every plan) | still test-first, one example per test, red seen before green |
| Aggregate style | state-based `Bike` with memento `Snapshot`, `Version` optimistic locking | plans D2, D4 |
| Unknown bike | `Bike.unknown(id)`: in circulation, not broken; Fleet Maintenance keeps no copy of the fleet | fleet-operations plan D8 |
| Who decides a withdrawal | the domain: `reportFault` raises `WithdrawalRequested` for `FLEET_ISSUE` / `TECHNICIAN` faults (Rentals already withdrew the others); use cases call `BikeWithdrawals` | no rule in the use case |
| Order of hand-offs | fleet issue: record, then withdraw (failure logged, D6); technician report: withdraw, then record (404 → UnknownBike, D5); return: Rentals first, then Bike Fixed, then incidents (failures logged, D5) | plans |
| Reservation period | Rentals' answer (`expiresAt`) is stored; never computed here | reserves plan D5 |
| Maintenance start | on Rentals' `BikeUnlocked` (technicians), never on the unlock's REST answer | starts plan D5 |
| Ports passed to aggregate methods | `reserveFor(...).through(ServiceReservations)`, `unlockFor(...).through(ServiceUnlocks)`, `returnToCirculation(...).through(CirculationReturns)` | the rule check must happen before the call, and the record only after Rentals accepted |
| Concurrency | event-driven use cases retry a lost optimistic-locking race up to 3 times (`Retrying`) | async listeners for one bike may run in parallel |
| Read side | `InMemoryBikes` implements `Bikes` and `BrokenBikesReadModel`; whereabouts asked from Rentals per query (`BikeWhereabouts`) | reserves plan D6 |
| Error names | exception class names are the error codes (`ErrorResponse.of`) | `NotBroken`, `MaintenanceNotStarted`, `AlreadyReserved`, `NotYourReservation`, `UnknownBike`, ... |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rentals | Event | Bike Withdrawn (all reasons but `REQUESTED`) | `rentalsctxacl/BikeWithdrawnListener` → `ReportBikeBrokenUseCase` |
| Rentals | Event | Bike Unlocked (technician) | `rentalsctxacl/BikeUnlockedListener` → `StartMaintenanceUseCase` |
| Rentals | Event | Reservation Expired / Unlock Failed (technician) | `rentalsctxacl/ReservationExpiredListener`, `UnlockFailedListener` → `ReleaseTechnicianReservationUseCase` |
| Customer Support | Event | Fleet Issue Reported | `customersupportctxacl/FleetIssueReportedListener` → `ReportBikeBrokenUseCase` |
| Technician app | Command | Report Fault | `ReportFaultUseCase`, `POST /fleet-maintenance/bikes/{bikeId}/fault-reports` |
| Technician app | Command | Reserve Broken Bike | `ReserveBrokenBikeUseCase`, `POST /fleet-maintenance/bikes/{bikeId}/reservation` → `200 {bikeId, technicianId, reservedUntil}` |
| Technician app | Command | Unlock Broken Bike | `UnlockBrokenBikeUseCase`, `POST /fleet-maintenance/bikes/{bikeId}/unlock` → `200 {bikeId, outcome}` |
| Technician app | Command | Return Bike to Circulation | `ReturnBikeToCirculationUseCase`, `POST /fleet-maintenance/bikes/{bikeId}/return-to-circulation` |
| Technician app | Query | Broken Bike / Broken Bikes | `BrokenBikeQueryHandler`, `BrokenBikesQueryHandler`; `GET /fleet-maintenance/bikes/{bikeId}`, `GET /fleet-maintenance/broken-bikes` |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Withdraw Bike | Command | Rentals | `BikeWithdrawals` | `rentalsctxacl/RentalsBikeWithdrawalsAdapter` |
| Reserve Bike for service | Command | Rentals | `ServiceReservations` | `rentalsctxacl/RentalsServiceReservationsAdapter` |
| Unlock Bike (technician) | Command | Rentals | `ServiceUnlocks` | `rentalsctxacl/RentalsServiceUnlocksAdapter` |
| Make Bike Available | Command | Rentals | `CirculationReturns` | `rentalsctxacl/RentalsCirculationReturnsAdapter` |
| Bike Availability | Query | Rentals | `api.queries.BikeWhereabouts` | `rentalsctxacl/RentalsBikeWhereaboutsAdapter` |
| Resolve Fleet Issue | Command | Customer Support | `FleetIssueResolutions` | `customersupportctxacl/CustomerSupportFleetIssueResolutionsAdapter` |
| Bike Reported Broken, Maintenance Started, Bike Fixed (+ Withdrawal Requested, Incident Linked, Bike Reserved For Technician, Technician Reservation Released) | Event | none outside | `BikeEvent` | not published (no listener) |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Broken bike / under maintenance / back in circulation | `BikeCondition` | domain, `BrokenBikeView.status` |
| Fault report, fault source | `FaultReport`, `FaultSource` | domain, `ReportBikeBroken.faultSource`, `BrokenBikeView.faultSource` |
| Incident link | `IncidentId`, `Bike.incidents()` | domain, `BrokenBikeView.incidentIds` |
| Technician | `TechnicianId` | domain, api records |
| Technician reservation | `TechnicianReservation` | domain, `BrokenBikeView.reservedUntil` |
| Maintenance, maintenance started | `Maintenance`, `BikeEvent.MaintenanceStarted` | domain |
| Bike fixed | `BikeEvent.BikeFixed`, `CirculationReturn`, `Place` | domain, `ReturnBikeToCirculation` |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| A bike withdrawn by Rentals is registered broken | green | `Bike.reportFault` | `FaultReportTest#bikeWithdrawnForFailedLockBecomesBroken`, `#bikeNotReturnedBecomesBroken` |
| A fleet-issue incident makes the bike broken and withdraws it | green | `Bike.reportFault` (`WithdrawalRequested`) | `FaultReportTest#fleetIssueMakesBikeBrokenAndRequestsWithdrawal` |
| A technician's fault report makes the bike broken and withdraws it | green | `Bike.reportFault` | `FaultReportTest#technicianFaultReportMakesBikeBrokenAndRequestsWithdrawal` |
| A duplicate fault report changes nothing; an incident is only linked | green | `Bike.reportFault` | `FaultReportTest#technicianFaultReportOnBrokenBikeChangesNothing`, `#fleetIssueOnBrokenBikeOnlyLinksIncident`, `#fixedBikeCanBreakAgain` |
| Only a broken bike can be reserved by a technician | green | `Bike.reserveFor` | `TechnicianReservationTest#technicianReservesBrokenBikeUntilRentalsSays`, `#technicianCannotReserveBikeThatIsNotBroken`, `#secondTechnicianCannotReserveReservedBike` |
| An expired reservation leaves the bike broken | green | `Bike.releaseReservationOf` | `TechnicianReservationTest#expiredReservationLeavesBikeBroken`, `#releasingAnotherTechniciansReservationChangesNothing` |
| A technician's unlock starts maintenance | green | `Bike.unlockFor`, `Bike.startMaintenanceBy` | `MaintenanceTest#technicianUnlockStartsMaintenance`, `#unlockOfBrokenBikeIsForwardedToRentals`, `#unlockOfBikeThatIsNotBrokenIsRejected`, `#repeatedUnlockReportChangesNothing` |
| A failed unlock leaves the bike broken | green | `Bike.releaseReservationOf` | `MaintenanceTest#failedUnlockLeavesBikeBrokenAndUnreserved` |
| A bike returns to circulation only after maintenance has started | green | `Bike.returnToCirculation` | `CirculationReturnTest#returnBeforeMaintenanceStartedIsRejected`, `#returnOfBikeThatIsNotBrokenIsRejected` |
| Returning a bike under maintenance fixes it | green | `Bike.returnToCirculation` | `CirculationReturnTest#returnAtStationFixesBike`, `#returnAtPositionFixesBike`, `#anyTechnicianMayReturnBike`, `#fixedBikeNamesItsLinkedIncidents`, `#bikeIsNotFixedWhenRentalsRefuses` |

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| none (the CML already has both `[D,ACL]` relationships) | | |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/
  package-info.java
  api/usecases/  ReportBikeBroken(+UseCase), ReportFault(+UseCase), ReserveBrokenBike(+UseCase),
                 TechnicianReservationReceipt, UnlockBrokenBike(+UseCase), UnlockResult, StartMaintenance(+UseCase),
                 ReleaseTechnicianReservation(+UseCase), ReturnBikeToCirculation(+UseCase), Retrying
  api/queries/   BrokenBikeQuery, BrokenBikesQuery, BrokenBikeView, BrokenBikeQueryHandler, BrokenBikesQueryHandler,
                 BrokenBikesReadModel, BikeWhereabouts
  domain/bike/   Bike, BikeEvent, Bikes, BikeId, TechnicianId, IncidentId, FaultDescription, FaultSource, FaultReport,
                 BikeCondition, TechnicianReservation, Maintenance, CirculationReturn, Place, FluentLanguage,
                 ports BikeWithdrawals, ServiceReservations, ServiceUnlocks, CirculationReturns, FleetIssueResolutions,
                 exceptions NotBroken, UnderMaintenance, MaintenanceNotStarted, AlreadyReserved, NotYourReservation,
                 ReservationRefused, UnlockRefused, CirculationReturnRefused, UnknownStation, UnknownBike,
                 FleetIssueNotResolved, CollaboratorUnavailable
  infra/adapters/primary/ FleetMaintenanceController
  infra/adapters/primary/rentalsctxacl/ package-info, BikeWithdrawnListener, BikeUnlockedListener,
                 ReservationExpiredListener, UnlockFailedListener
  infra/adapters/primary/customersupportctxacl/ package-info, FleetIssueReportedListener
  infra/adapters/secondary/ InMemoryBikes
  infra/adapters/secondary/rentalsctxacl/ package-info, RentalsResource, RentalsBikeWithdrawalsAdapter,
                 RentalsServiceReservationsAdapter, RentalsServiceUnlocksAdapter, RentalsCirculationReturnsAdapter,
                 RentalsBikeWhereaboutsAdapter
  infra/adapters/secondary/customersupportctxacl/ package-info, CustomerSupportFleetIssueResolutionsAdapter
  infra/config/FleetMaintenanceContextConfig.java
src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/
  FleetMaintenanceArchitectureTest, FleetMaintenanceSpecTestBase,
  FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest, TechnicianReportsFaultSpecTest,
  TechnicianReservesBrokenBikeSpecTest, TechnicianStartsMaintenanceSpecTest,
  TechnicianReturnsFixedBikeToCirculationSpecTest,
  domain/bike/BikeFixture, FaultReportTest, TechnicianReservationTest, MaintenanceTest, CirculationReturnTest,
  infra/adapters/secondary/InMemoryBikesTest
src/test/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/RiderReportsIncidentSpecTest (E3.1, E3.3 completed)
```

## Stubs and follow-ups
- No stubs.
- Follow-up: a fleet-issue withdrawal call that fails is only logged (D6); a retry (e.g. a scheduled reconciliation
  of broken bikes Rentals still offers) isn't built.
- Follow-up: failed incident resolutions after a fix are only logged (returns plan D5).
- Fleet Maintenance publishes no integration events (none has a listener yet).
- No CML change.

## Changelog
### 2026-11-08: scaffolded and implemented
- Added: the whole module listed under Files.
- Triggered by: intents fleet-operations-takes-broken-bike-out-of-circulation, technician-reports-fault,
  technician-reserves-broken-bike, technician-starts-maintenance, technician-returns-fixed-bike-to-circulation;
  rider-reports-incident S6.
- Decision: one aggregate `Bike` for all rules (each rule reads the bike's condition); the withdrawal decision is a
  domain event (`WithdrawalRequested`), the Rentals calls that must precede a record are ports passed to the
  aggregate's methods.
- Decision: derived tests beyond the spec examples, each guarding a hole: `fixedBikeCanBreakAgain`,
  `releasingAnotherTechniciansReservationChangesNothing`, `unlockOfBrokenBikeIsForwardedToRentals`,
  `unlockOfBikeThatIsNotBrokenIsRejected`, `repeatedUnlockReportChangesNothing`, `bikeIsNotFixedWhenRentalsRefuses`;
  REST-level `fault_report_on_unknown_bike_is_rejected`, `fault_report_without_description_is_bad_request`,
  `broken_bikes_list_shows_where_they_are`.

#### TDD log (checkpoint loop off; skeleton `Bike` with commands throwing `UnsupportedOperationException`, all 24 tests written and seen red, then implemented one rule group at a time; all implemented by Claude)
| # | Scenario | Block | Test | Red seen | Green |
|---|---|---|---|---|---|
| 1-2 | fleet-operations E1.1, E1.2 | `Bike.reportFault` | `FaultReportTest#bikeWithdrawnForFailedLockBecomesBroken`, `#bikeNotReturnedBecomesBroken` | yes (UnsupportedOperation) | yes |
| 3 | fleet-operations E2.1 | `Bike.reportFault` | `#fleetIssueMakesBikeBrokenAndRequestsWithdrawal` | yes | yes |
| 4 | fleet-operations E3.1 | `Bike.reportFault` (link) | `#fleetIssueOnBrokenBikeOnlyLinksIncident` | yes | yes |
| 5-6 | technician-reports-fault E1.1, E3.1 | `Bike.reportFault` | `#technicianFaultReportMakesBikeBrokenAndRequestsWithdrawal`, `#technicianFaultReportOnBrokenBikeChangesNothing` | yes | yes |
| 7 | derived: break again after fix | `Bike.reportFault` | `#fixedBikeCanBreakAgain` | yes | yes (after cycle 21) |
| 8-10 | technician-reserves E1.1, E2.1, E3.1 | `Bike.reserveFor` | `TechnicianReservationTest#technicianReservesBrokenBikeUntilRentalsSays`, `#technicianCannotReserveBikeThatIsNotBroken`, `#secondTechnicianCannotReserveReservedBike` | yes (E2.1: assertion failure, the others UnsupportedOperation) | yes |
| 11-12 | technician-reserves E4.1 + derived | `Bike.releaseReservationOf` | `#expiredReservationLeavesBikeBroken`, `#releasingAnotherTechniciansReservationChangesNothing` | yes | yes |
| 13-17 | technician-starts E1.1, E4.1 + 3 derived | `Bike.startMaintenanceBy`, `unlockFor`, `releaseReservationOf` | `MaintenanceTest#*` | yes | yes |
| 18-24 | technician-returns E1.1, E1.2, E1.3, E2.1, E2.2, E3.1 + derived | `Bike.returnToCirculation` | `CirculationReturnTest#*` | yes | yes |
- After all domain tests green: `Version` + `Snapshot`, port `Bikes`, `InMemoryBikes` with optimistic locking,
  `InMemoryBikesTest` (2 tests); then use cases, listeners, ACL adapters, REST, wiring and the acceptance tests.
