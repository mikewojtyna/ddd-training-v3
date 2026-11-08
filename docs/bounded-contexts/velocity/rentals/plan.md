# Rentals bounded context: implementation plan

- **System / package**: `velocity.rentals`
- **Mode**: scaffold (the old BikeStation-based module was removed in commit 4702217)
- **Source canvas**: `docs/bounded-contexts/velocity/rentals/canvas.md`
- **Last updated**: 2026-11-08
- **Status**: implemented

## Review notes
- 2026-11-08: approved via docs/plans/fleet-manager-adds-bike-to-fleet.md, docs/plans/rider-reserves-bike.md,
  docs/plans/rider-unlocks-reserved-bike.md, docs/plans/fleet-operations-releases-expired-reservations.md,
  docs/plans/rider-returns-bike-at-station.md, docs/plans/rider-ends-ride-in-drop-zone.md,
  docs/plans/fleet-operations-ends-overdue-rental.md, docs/plans/fleet-operations-withdraws-bike-signalling-fault.md, and
  the Rentals steps of docs/plans/fleet-operations-takes-broken-bike-out-of-circulation.md,
  docs/plans/technician-reserves-broken-bike.md, docs/plans/technician-starts-maintenance.md,
  docs/plans/technician-returns-fixed-bike-to-circulation.md and docs/plans/finance-blocks-rider-with-invalid-card.md
  (autonomous run, 2026-11-08). Structural decisions and the TDD-loop choice (off, D1 in every plan) come from those plans.
- Decisions the intent plans left open, taken here (autonomous run): rows marked *(new)* in the Decisions table.

## Canvas summary
Rentals gets a bike into someone's hands in seconds and guarantees one bike, one active holder: availability, rider and
technician reservations, unlocks, rental sessions and every way a session ends. Core domain, revenue, custom built;
roles: Execution Model, Enforcer.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards Accounts (Rental Eligibility) | yes: port `domain/bike/RentalEligibility`, adapter `infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter` over REST (`GET /accounts/riders/{riderId}/rental-eligibility`), own response record | CML `Accounts [U,OHS,PL]->[D,ACL] Rentals`; integration-contracts.md |
| ACL towards the lock hardware | yes: port `domain/bike/BikeLocks`, simulated adapter `infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks` (`registerFaulty`, `registerRepaired`, `scriptNext`) | integration-contracts.md; external party |
| Boundary exceptions | none | |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | |
| TDD checkpoint loop | off (trainer's autonomy request, 2026-11-08, plans' D1) | domain still built test-first, red seen before green |
| Aggregate style | state-based (plans' D2) | |
| Aggregate | one `Bike` per bike holding availability, hold (`Free`/`Reserved`/`RentalSession`/`ServiceUnlock`), position/station and a pending fault | "one bike, one active holder" is a per-bike invariant (plans' default) |
| New bikes | factory `Fleet` with port `FleetRegister` (implemented by the repository) and the reference data `Stations` | R2 (only once) needs all bikes, R3 (only at a station) the station catalogue |
| Stations *(new)* | domain value `Stations.reference()`: `Central` 52.2300/21.0110, `Old Town` 52.2490/21.0122, `Riverside` 52.2400/21.0300; the station id is its name | integration-contracts.md lists them by name |
| One rider, one bike *(new)* | the use case passes a `RiderStanding(eligibility, holdsAnotherBike)` into `reserveForRide`; `holdsAnotherBike` comes from `BikeRepository.holdsABike(rider, now)` (a reservation that still holds, or a session) | rule spans bikes; the bike still decides. **Accepted race:** two concurrent reservations by the same rider on two bikes can both pass |
| Eligibility unknown *(new)* | `RentalEligibilityUnknown` → `503` | fail closed (plan `rider-reserves-bike` D7) |
| Locks *(new)* | the bike calls `BikeLocks` itself (`...using(locks)`) after its own checks | a rejected unlock or return never touches the lock |
| Distances *(new)* | haversine, Earth radius 6 371 008.8 m, rounded to centimetres; "near" = ≤ 50.00 m | exactly 50 m is near (spec Q1) |
| Unlock answer *(new)* | `200 {bikeId, outcome: UNLOCKED\|UNLOCK_FAILED, sessionId?}` | integration-contracts.md Additions override the plan's `SESSION_STARTED` |
| `BikeWithdrawn.faultDescription` *(new)* | added as the last field of `api.events.BikeWithdrawn`: rider's description or bike's fault code | integration-contracts.md Additions |
| Withdrawal during a ride *(new)* | a client's withdrawal withdraws at once (event published), the ride goes on; a fault signal during a ride is kept pending and withdraws at the session end; one Bike Withdrawn per bike until it is made available | DE-25, DE-86, plan D6 |
| Overdue end *(new)* | session ends at started + 12 h; position and station become unknown (null); Bike Withdrawn *not returned* at the check's now | DE-21, DE-82 |
| Make Bike Available *(new)* | only a withdrawn bike unlocked for a technician (`BikeNotUnlockedForService` otherwise); Rentals doesn't call `BikeLocks` (the technician leaves the bike locked) | canvas "Rejected" cases; no lock outcome is specified |
| Time-driven work | `ReleaseExpiredReservationsUseCase` and `EndOverdueRentalSessionsUseCase`, each bike in its own load-execute-save, optimistic-locking conflicts skipped; triggered every second by `infra/adapters/primary/RentalsClockJobs` | plans' D6 |
| Persistence | `InMemoryBikeRepository` (memento `Bike.Snapshot`, `Version` optimistic locking; a second new bike with the same id fails too), also the read model `RentalsReadModel` | plans' D4 |
| Published language | `IntegrationEventsPublisher` maps domain events to `api.events`: `RentalSessionEnded`, `BikeWithdrawn`, `ReservationExpired` and `UnlockFailed` for every holder, `BikeUnlocked` (technicians only: a rider's unlock is a Rental Session Started) | integration-contracts.md |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Fleet manager | Command | Add Bike to Fleet | `AddBikeToFleet` + `AddBikeToFleetUseCase`; `POST /rentals/bikes` → `201` |
| Rider app / Fleet Maintenance | Command | Reserve Bike (ride / service) | `ReserveBike` + `ReserveBikeUseCase` → `BikeReservation`; `POST /rentals/bikes/{bikeId}/reservations` → `201` |
| Rider app / Fleet Maintenance | Command | Unlock Bike | `UnlockBike` + `UnlockBikeUseCase` → `UnlockOutcome`; `POST /rentals/bikes/{bikeId}/unlock` |
| Rider app | Command | Return Bike at Station | `ReturnBikeAtStation` + `ReturnBikeAtStationUseCase` → `SessionEnd`; `POST .../station-return` |
| Rider app | Command | End Ride Here | `EndRideHere` + `EndRideHereUseCase` → `SessionEnd`; `POST .../free-floating-return` |
| Bike telemetry | Command | Signal Fault | `SignalFault` + `SignalFaultUseCase`; `POST .../fault-signals` → `202` |
| Fleet Maintenance | Command | Withdraw Bike | `WithdrawBike` + `WithdrawBikeUseCase`; `POST .../withdrawal` |
| Fleet Maintenance | Command | Make Bike Available | `MakeBikeAvailable` + `MakeBikeAvailableUseCase`; `POST .../availability` |
| Fleet operations (clock) | Command | Expire Reservations | `ReleaseExpiredReservations` + `ReleaseExpiredReservationsUseCase`; `RentalsClockJobs` |
| Fleet operations (clock) | Command | End Overdue Sessions | `EndOverdueRentalSessions` + `EndOverdueRentalSessionsUseCase`; `RentalsClockJobs` |
| Anyone | Query | Bike Availability | `BikeAvailabilityQuery`, `BikeAvailability`, `BikeAvailabilityQueryHandler`; `GET /rentals/bikes/{bikeId}` (`404` unknown) |
| Rider app | Query | Rider's Active Rental | `RidersActiveRentalQuery`, `RidersActiveRental`, `RidersActiveRentalQueryHandler`; `GET /rentals/riders/{riderId}/active-rental` (`404` none) |

Rejections: `409 {error, message}` with `BikeAlreadyInFleet`, `UnknownStation`, `TooFarFromBike`, `RiderNotEligible`,
`OneRiderOneBike`, `BikeNotAvailable`, `BikeNotWithdrawn`, `BikeAlreadyHeld`, `NotReservationHolder`, `NoRunningSession`,
`BikeNotUnlockedForService`, `OptimisticLockingException`; `404 BikeNotFound`; `400` malformed input; `503 RentalEligibilityUnknown`.

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Rental Eligibility | Query | Accounts | `domain/bike/RentalEligibility` | `AccountsRentalEligibilityAdapter` (`infra/adapters/secondary/accountsctxacl`) |
| Unlock / Lock | Command | Bike locks | `domain/bike/BikeLocks` | `SimulatedBikeLocks` (`infra/adapters/secondary/bikelocksacl`) |
| Rental Session Ended, Bike Withdrawn, Bike Unlocked, Reservation Expired, Unlock Failed | Event | Settlement, Fleet Maintenance | n/a | `infra/adapters/secondary/IntegrationEventsPublisher` → `api.events` |
| Bike Added to Fleet, Bike Reserved, Reservation Cancelled, Rental Session Started, Bike Made Available | Event | none outside Rentals | n/a | domain events only (`BikeEvent`) |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Fleet | `Fleet` (factory), `FleetRegister` | domain |
| Bike | `Bike` (aggregate root), `BikeId` | domain, api records |
| Available / withdrawn | `Bike.Availability` | domain, `BikeAvailability.availability` |
| Holder (rider, technician) | `Holder`, `Holder.HolderType` | domain, api records |
| Reservation / unlock / rental session | `Bike.Hold.Reserved`, `Bike.Hold.ServiceUnlock`, `Bike.Hold.RentalSession`, `SessionId` | domain, `BikeAvailability.hold`, `RidersActiveRental` |
| End reason | `EndReason` | domain, `SessionEnd`, `api.events.RentalSessionEnded` |
| Withdrawal reason | `WithdrawalReason` | domain, `api.events.BikeWithdrawn` |
| Station | `Station`, `Station.StationId`, `Stations` | domain |
| Near | `Bike.NEAR_METRES`, `Position.metresTo` | domain |
| Maximum rental time | `Bike.MAXIMUM_RENTAL_TIME` | domain |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| A bike is added at a station, only once, only at a station | green | `Fleet` (factory) | `FleetTest` (3) |
| An eligible rider within 50 m reserves an available, free bike for 2 minutes; one rider, one bike | green | `Bike` + `RiderStanding` | `BikeReservationTest` (8) |
| Only the holder of a reservation that still holds unlocks; rider unlock starts a session; failed lock releases; ineligible rider rejected | green | `Bike` + port `BikeLocks` | `BikeUnlockTest` (8) |
| An unused reservation expires after its end, availability unchanged | green | `Bike` | `ReservationExpiryTest` (7) |
| Station return, lock failure, reported fault | green | `Bike` + port `BikeLocks` | `StationReturnTest` (7) |
| Free-floating return, lock failure anywhere | green | `Bike` | `FreeFloatingReturnTest` (5) |
| Overdue session ends at the 12-hour mark, bike not returned | green | `Bike` | `OverdueSessionTest` (5) |
| Fault signal: at once outside a ride, at the session end during one, never twice | green | `Bike` | `FaultSignalTest` (7) |
| A client's withdrawal | green | `Bike` | `BikeWithdrawalTest` (4) |
| Technician reservations, unlocks, making a bike available | green | `Bike` | `TechnicianServiceTest` (13) |

### Rules awaiting GWT
- none

## Deferred until tactical blocks are built
- Nothing left: use cases, query handlers, REST adapter, scheduled jobs, ACL adapters, repository, publisher and wiring were generated after the domain was green.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| none | | |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/
  package-info.java
  api/events/ (published language; BikeWithdrawn gained faultDescription)
  api/usecases/ AddBikeToFleet(UseCase), ReserveBike(UseCase), BikeReservation, UnlockBike(UseCase), UnlockOutcome,
    ReleaseExpiredReservations(UseCase), ReturnBikeAtStation(UseCase), EndRideHere(UseCase), SessionEnd,
    EndOverdueRentalSessions(UseCase), SignalFault(UseCase), WithdrawBike(UseCase), MakeBikeAvailable(UseCase),
    BikeCommands, Translation
  api/queries/ BikeAvailabilityQuery, BikeAvailability, BikeAvailabilityQueryHandler, RidersActiveRentalQuery,
    RidersActiveRental, RidersActiveRentalQueryHandler, RentalsReadModel
  domain/bike/ Bike, BikeEvent, BikeId, Holder, Position, Station, Stations, SessionId, EndReason, WithdrawalReason,
    RiderStanding, Fleet, FleetRegister, BikeRepository, BikeLocks, RentalEligibility, FluentLanguage, exceptions
  infra/adapters/primary/ RentalsController, RentalsClockJobs
  infra/adapters/secondary/ InMemoryBikeRepository, IntegrationEventsPublisher, accountsctxacl/, bikelocksacl/
  infra/config/RentalsContextConfig
src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/
  RentalsArchitectureTest, RentalsDriver, RentalsSpecTestBase, <IntentCamelCase>SpecTest (8),
  RentalsServiceForFleetMaintenanceRestTest
  domain/bike/ (10 test classes, BikeFixture, FakeBikeLocks)
  infra/adapters/secondary/InMemoryBikeRepositoryTest, accountsctxacl/AccountsRentalEligibilityAdapterTest
```

## Stubs and follow-ups
- Fleet Maintenance (not yet in code) will consume `BikeWithdrawn` (all reasons except `REQUESTED`), `BikeUnlocked`,
  `ReservationExpired`, `UnlockFailed`; Settlement will consume `RentalSessionEnded`. Rentals' tests assert the events
  are published; the hand-offs are tested by those contexts' plans.
- `velocity/CLAUDE.md` still describes the old BikeStation code in its Rentals section; the `ddd-tactical-building-blocks`
  and `tdd` skills still name `BikeStation`/`BikeStationTest` as reference (flagged in `fleet-manager-adds-bike-to-fleet`).
- Open questions from the canvas: repeated failed unlocks; warning before the maximum rental time.

## Changelog
### 2026-11-08: scaffolded (Rentals wave, autonomous run)
- Added: the whole module above, from the canvas and the twelve intent plans.
- Changed: `api.events.BikeWithdrawn` gained the optional last field `faultDescription`; canvas updated with every
  plan's canvas diff (Rentals halves).
- Triggered by: the intent plans listed in the review notes.

#### TDD log (checkpoint loop off; every implementation by Claude)
Tests were written first in four groups; each group's tests ran red (assertion failures against compile-only stubs
returning `DomainEvents.empty()` / no checks) before the production code was written, then green.
| Group | Scenario(s) | Block | Test | Red seen | Implemented by | Green |
|---|---|---|---|---|---|---|
| 1 | add bike: new, twice, not a station | `Fleet` | `FleetTest` (3) | yes, 3/3 | Claude | yes |
| 1 | rider reservation E1.1-E5.2 | `Bike.reserveForRide` | `BikeReservationTest` (8) | yes, 8/8 | Claude | yes |
| 1 | rider unlock E1.1-E4.1 | `Bike.unlockForRide` | `BikeUnlockTest` (8) | yes, 8/8 | Claude | yes |
| 1 | expiry E1.1-E3.2, technician E2.1/E2.2 | `Bike.releaseIfExpired` | `ReservationExpiryTest` (7) | 6/7; `reservationReleasedByFailedUnlockDoesNotExpire` passed at once (nothing to expire on a stub) | Claude | yes |
| 2 | station return E1.1-E5.2 | `Bike.returnAtStation` | `StationReturnTest` (7) | 6/7; `returnAtUnknownStationIsRejected` passed at once (`Stations` already rejects) | Claude | yes |
| 2 | free-floating return E1.1-E3.2 | `Bike.endRideHere` | `FreeFloatingReturnTest` (5) | yes, 5/5 | Claude | yes |
| 2 | overdue E1.1-E3.1 | `Bike.endIfOverdue` | `OverdueSessionTest` (5) | 4/5; `sessionOfExactlyTwelveHoursStillRuns` passed at once (empty stub) | Claude | yes |
| 3 | fault signal E1.1-E4.1 + D6 combination | `Bike.signalFault` | `FaultSignalTest` (7) | 4/7; E3.1, E4.1 and the D6 case passed at once (no change expected / already guarded) | Claude | yes |
| 3 | client withdrawal | `Bike.withdraw` | `BikeWithdrawalTest` (4) | 2/4; free bike and ride-goes-on passed at once (the minimal `withdraw` the fixtures needed was written in group 1) | Claude | yes |
| 3 | technician unlock and make available | `Bike.unlockForService`, `makeAvailableAt` | `TechnicianServiceTest` (8 of 13) | yes, 8/8 | Claude | yes |
| 3 | technician reservations | `Bike.reserveForService` | `TechnicianServiceTest` (5 of 13) | no: passed at once, `reserveForService` was written in group 1 because the expiry fixture needed it | Claude | yes |
- Deviation, recorded honestly: cycles were batched per group (several tests written, all observed red, then implemented)
  instead of strictly one test per cycle; scenarios that passed at once are listed above.
- After the domain: `Version` + `Bike.Snapshot`, `InMemoryBikeRepository` (+ test), use cases, REST, jobs, ACLs, publisher,
  wiring, `RentalsArchitectureTest`; acceptance tests per intent (8 classes, 51 tests) and the Fleet Maintenance-facing
  REST test (18 tests).
