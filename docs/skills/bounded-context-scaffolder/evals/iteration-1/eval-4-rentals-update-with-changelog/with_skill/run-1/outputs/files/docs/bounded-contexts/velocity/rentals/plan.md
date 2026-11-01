# Rentals bounded context: implementation plan

- **System / package**: `velocity.rentals`
- **Mode**: update
- **Source canvas**: `docs/bounded-contexts/velocity/rentals/canvas.md` (copied from `.claude/skills/bounded-context-scaffolder/evals/files/rentals-update-canvas.md`)
- **Last updated**: 2026-11-01

## Canvas summary
Rentals owns bike availability at stations, renting and returning bikes, and the maximum rental time, so riders can
always find and return a bike and bikes are never held forever. Core domain, revenue, custom built. Role: Execution
Model (from `docs/model-traits-worksheet.md`).

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default; existing `package-info.java` already says so |
| Mode | update | `velocity/rentals` exists |
| Aggregate | keep `BikeStation` as the only aggregate | every rule's Given is "a bike station with ..." state; availability of a bike is checked inside one station |
| `BikeStation.returnBike` | implemented (workshop stub replaced) | the canvas now has two strict GWT rules for returning; the method signature `returnBike(Bike)` was kept, it already reads like the canvas ("bike01 is returned to the station"), so no curried continuation was needed |
| `rent(bike)` | now rents the **requested** bike instead of any available bike | returning works by bike id; with the old `findAny()` renting bike02 could mark bike01 as rented, and the return of bike02 would then be rejected. All existing tests still pass |
| `BikeReturned` domain event | now carries `to` (station id) and `bike` (bike id) | the integration event for Pricing needs to say which bike was returned where |
| `RentBikeUseCase` | left untouched (workshop stub) | canvas lists Rent Bike as "existing"; the stub is deliberate per `velocity/CLAUDE.md` |
| Return use case style | command record `ReturnBike` + `handle(...)` | skill rule for new code; the existing `RentBikeUseCase` takes domain ids as method parameters instead |
| Query read model | answered from the aggregate via `BikeStationRepo` (`BikeStation.availableBikes()`) | no event carries enough data for a projection (`BikeRented` has no bike id, `placeNew` emits nothing) |
| ACL towards Pricing | n/a | outbound is an event we publish in our own language (`api.events.BikeReturned`); no port into Pricing |
| REST adapters for Rider app | not created | the canvas doesn't describe the endpoints; follow-up |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rider app | Command | Rent Bike (existing) | `api/usecases/RentBikeUseCase` (unchanged stub), `BikeStation.rent(bike).by(borrower).forDuration(duration)` |
| Rider app | Command | Return Bike (NEW) | `api/usecases/ReturnBike`, `api/usecases/ReturnBikeUseCase`, `BikeStation.returnBike(bike)` |
| Rider app | Query | Available Bikes At Station (NEW) | `api/queries/AvailableBikesAtStation`, `AvailableBikesAtStationView`, `AvailableBikesAtStationQueryHandler`, port `AvailableBikesReadModel`, adapter `infra/adapters/secondary/BikeStationAvailableBikesReadModel` |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Bike Returned | Event | Pricing | n/a (published event) | domain `BikeRentalEvent.BikeReturned` → `infra/adapters/secondary/IntegrationEventsPublisher` → `api/events/BikeReturned` |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Bike Station | `BikeStation`, `BikeStationId` | aggregate root, identifier |
| Bike | `Bike`, `BikeId` | VO, identifier (entity inside the aggregate: `BikeInStation`) |
| Borrower | `Borrower`, `BorrowerId` | VO, identifier |
| Rental Duration | `RentalDuration` | VO |

### Business rules → aggregates
| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| Rent available bike (1 available bike, rent for 6 h → rented) | a bike can be rented only while available | `BikeStation` | `BikeStationTest#test0` (existing) |
| Max rental duration (rent for 12 h → rejected) | rental duration ≤ 11 h | `BikeStation` | `BikeStationTest#test3` (existing) |
| Return rented bike (bike01 rented by George, returned → returned, available again) | a rented bike becomes available on return | `BikeStation` | `BikeStationTest#returnRentedBike` (new) |
| Return bike that is not rented (bike01 available, returned → rejected) | only a rented bike can be returned; `BikeNotRentedException` | `BikeStation` | `BikeStationTest#returnBikeThatIsNotRented` (new) |

Existing tests `test1` (0 bikes → not rented) and `test2` (rent twice → second rejected) aren't in the canvas but are kept.

### Rules awaiting GWT
- **Max rental duration boundary.** UL says "max 11 hours", but the code checks `duration.toHours() > 11`, which truncates, so 11 h 30 min is accepted. Suggested GWT: *given a bike station with 1 available bike, when George rents a bike for 11 hours 30 minutes, then the rental is rejected*. Status: awaiting confirmation; code unchanged.
- **Return of a bike unknown to the station** (e.g. rented at another station). Currently rejected with `BikeNotRentedException`, because the station has no record of it. Suggested GWT: *given station "Warsaw02" without bike "bike01", and bike "bike01" rented by George at "Warsaw01", when bike "bike01" is returned to "Warsaw02", then ...?* Status: awaiting decision.

No internal contradictions found in the canvas's GWT examples.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Outbound event **Bike Returned** → **Pricing** | Rentals publishes **Rental Session Ended** (start, end, location, reason) → **Settlement**; Pricing is called by Settlement | Partly: "Pricing reacting to Rental Session Ended" is accepted, but the message name and payload differ |
| No Rentals → Fleet Maintenance relationship | Rentals [OHS,PL] → Fleet Maintenance: BikeAvailability (withdraw / make available, reserve / unlock, faults) | No |
| No query to Accounts | Accounts → Rentals: AccountStatus ("is account active?") | No |
| Language: rent, return, borrower, rental duration | Language: reservation, unlock, rental session, available / withdrawn | No (terminology difference) |
| Max rental duration rejects the rent up front | Rentals detects max rental time and ends the session automatically; Pricing adds a penalty | No |
| Purpose has no reserve/unlock, withdrawing, or locking | CML responsibilities include reserve and unlock, station returns and locking, withdrawing faulty bikes | No |

## Files
```
docs/bounded-contexts/velocity/rentals/
  canvas.md                                    (new, copy of the input canvas)
  plan.md                                      (new)
src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/
  package-info.java                            (changed: canvas javadoc)
  api/usecases/ReturnBike.java                 (new)
  api/usecases/ReturnBikeUseCase.java          (new)
  api/queries/AvailableBikesAtStation.java     (new)
  api/queries/AvailableBikesAtStationView.java (new)
  api/queries/AvailableBikesReadModel.java     (new)
  api/queries/AvailableBikesAtStationQueryHandler.java (new)
  api/events/BikeReturned.java                 (new)
  domain/bikestation/BikeStation.java          (changed)
  domain/bikestation/BikeRentalEvent.java      (changed: BikeReturned fields)
  domain/bikestation/BikeNotRentedException.java       (new)
  domain/bikestation/BikeStationNotFoundException.java (new)
  infra/adapters/secondary/InMemoryBikeStationRepo.java            (new)
  infra/adapters/secondary/BikeStationAvailableBikesReadModel.java (new)
  infra/adapters/secondary/IntegrationEventsPublisher.java         (new)
  infra/config/RentalsContextConfig.java       (new)
src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bikestation/
  BikeStationTest.java                         (changed: 2 new tests)
```

## Stubs and follow-ups
- Pricing doesn't exist in code; nothing consumes `api.events.BikeReturned` yet. Its payload (station id, bike id) is a minimum; Pricing will likely need borrower, rental start/end or duration. Canvas doesn't say.
- `RentBikeUseCase` is still a workshop stub and isn't wired as a bean.
- REST primary adapters for the Rider app (Return Bike, Available Bikes At Station) aren't created.
- Existing `test0`/`test2` don't assert the boolean from `hasAtLeastOneEventMatching`; they'd pass even without a `BikeRented` event.
- `src/main/cml/VeloCity-*.cml` and `velocity/velocity-context-map.md` don't reflect the canvas differences above; not changed.

## Changelog
### 2026-11-01: updated
- Added: Return Bike command (`ReturnBike`, `ReturnBikeUseCase`), `BikeStation.returnBike` implementation, `BikeNotRentedException`, `BikeStationNotFoundException`; Available Bikes At Station query (query, view, handler, read-model port and adapter, `BikeStation.availableBikes()`); `BikeStation.id()` with `@Identity`; integration event `api.events.BikeReturned` and `IntegrationEventsPublisher`; `InMemoryBikeStationRepo`; `RentalsContextConfig`; tests `returnRentedBike`, `returnBikeThatIsNotRented`; canvas javadoc in `package-info.java`; this plan and the canvas copy.
- Changed: `BikeRentalEvent.BikeReturned` now has `(BikeStationId to, BikeId bike)`; `BikeStation.rent` rents the requested bike instead of any available one; `package-info.java` purpose rewritten from the canvas (was "reserving and unlocking bikes, rental sessions, station returns").
- Removed (confirmed by user): none.
- Proposed removals (not applied): none. Replaced: the `UnsupportedOperationException` body of `BikeStation.returnBike` (a workshop stub), because the canvas now specifies it with strict GWT rules.
- Triggered by: canvas adds inbound Return Bike (command) and Available Bikes At Station (query), outbound Bike Returned event to Pricing, and the two return rules.
