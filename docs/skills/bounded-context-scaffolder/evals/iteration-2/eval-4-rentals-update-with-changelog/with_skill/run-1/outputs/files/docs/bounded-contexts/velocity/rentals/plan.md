# Rentals bounded context: implementation plan

- **System / package**: `velocity.rentals`
- **Mode**: update
- **Source canvas**: `docs/bounded-contexts/velocity/rentals/canvas.md` (copied from `.claude/skills/bounded-context-scaffolder/evals/files/rentals-update-canvas.md`, Q1; plus the restored "No available bike" rule, Q4)
- **Last updated**: 2026-11-01
- **Status**: implemented

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-01: first plan for Rentals (no earlier plan existed; the context was hand-written as a workshop example). Draft written, awaiting review. No code changed yet.
- 2026-11-01: **approved** by the user in the conversation ("Rentals plan reviewed - approved"). All open questions Q1-Q8 take the proposed defaults; the user explicitly confirmed filling in the `returnBike` and `RentBikeUseCase` workshop stubs (Q2). Canvas unchanged since the draft. No other plan changes. Status set to `approved`, then `implemented` after the build passed.

## Canvas summary
Rentals owns bike availability at stations, renting and returning bikes, and the maximum rental time, so riders can always find and return a bike and bikes are never held forever. Classified as **core / revenue / custom built**, with the **Execution Model** role from `docs/model-traits-worksheet.md` (it performs and tracks the rental job). All classification values and the role are valid worksheet values.

## Questions for the reviewer
Answered 2026-11-01: the user approved all proposed defaults (column "Answer").

| # | Question | Proposed default (= Answer: accepted) |
|---|---|---|
| Q1 | The canvas lives in the skill's eval folder. Move a copy to the default location `docs/bounded-contexts/velocity/rentals/canvas.md` so the plan and javadoc point to a stable file in the repo? | Yes, copy it there and point the plan and `package-info.java` to it. |
| Q2 | Implementing *Return Bike* and *Rent Bike* end to end replaces two **workshop stubs** that the VeloCity `CLAUDE.md` calls intentional: `BikeStation.returnBike` (`UnsupportedOperationException`) and `RentBikeUseCase` (steps as comments only). OK to fill them in? | Yes, implement them in place (the new canvas rules require `returnBike`). If the stubs must stay for a workshop, say so and the plan switches to keeping them and stopping at the domain tests. |
| Q3 | "Rental Duration: max 11 hours" vs. the code: `duration().toHours() > 11` truncates, so 11 h 59 min is **accepted** today. Which is right? See "Rules awaiting GWT". | Not changed until you confirm a boundary example. |
| Q4 | The code and tests enforce a rule the canvas no longer lists: *renting when no bike is available is rejected* (`BikeNotAvailableException`, `BikeStationTest#test1`, `#test2`). Was it dropped from the canvas on purpose? | Keep the code and tests (never deleted without confirmation); add the rule back to the canvas. |
| Q5 | Returning a bike to a **different** station than the one it was rented from: the canvas only covers "returned to the station" where it was rented. With `BikeStation` as the aggregate, another station doesn't know the bike. | Not modelled. A return of a bike the station doesn't know is rejected with the same `BikeNotRented` exception, because that station has no rented bike with that id. Please give a GWT example if cross-station returns must work. |
| Q6 | Payload of the published **Bike Returned** event for Pricing. The canvas gives no fields. Pricing will most likely need rental start/end times, which Rentals doesn't track (there is no clock in the model). | `stationId`, `bikeId`, `borrowerId` (strings). Times are left for a later canvas change. |
| Q7 | *Available Bikes At Station* result: list of bike ids, or just a count? | List of bike ids plus the station id; the count is derivable. |
| Q8 | The query read model needs to learn which bikes are placed in a station. `placeNew` returns `void` today and emits no event. | `placeNew` returns `DomainEvents` with a new `BikePlacedInStation` domain event (callers that ignore the result still compile). The in-memory read model is a projection of `BikePlacedInStation` / `BikeRented` / `BikeReturned`. Alternative: a read method on the aggregate, which breaks the "behaviour, no getters" rule. |

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Mode | update | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/` exists. |
| Name | `Rentals` | Canvas name equals the CML `BoundedContext Rentals` and the existing `@BoundedContext(name = "Rentals")`. |
| Modules | 1 module = 1 context | default, unchanged |
| Aggregate | keep `BikeStation` as the only aggregate | Every Given in the canvas ("a bike station with ...") is station state; renting and returning are checked against the bikes in one station. No new aggregate is needed. |
| Command style | keep the curried fluent API for rent (`station.rent(bike).by(borrower).forDuration(duration)`); keep the existing `returnBike(Bike)` signature | Keep the style already in the code (skill Step 5.3). The return command has no "by" or duration in the canvas, so no new fluent step. |
| Use-case style | keep `RentBikeUseCase.rent(BikeStationId, BikeId, BorrowerId, RentalDuration)`; add `ReturnBikeUseCase.returnBike(BikeStationId, BikeId)` in the same style | Existing style wins over the template's command records in update mode. Both get `@PrimaryPort` and the load, execute, save, publish flow. |
| Rent the *requested* bike | change `rent(bike)` to rent the given bike, not "any available bike" | Today `rent` ignores its `bike` argument. The new return rule ("bike01 was rented by George") needs to know which bike was rented. Existing tests still pass (each has one bike). |
| ACLs | none | The only outbound message is an event (Bike Returned → Pricing); events need no outbound ACL. No inbound messages come from another context. |
| Outbound event | domain event `BikeRentalEvent.BikeReturned` + integration event record `api/events/BikeReturned` (strings only), published by `IntegrationEventsPublisher` | Skill rule: keep the published language separate from the domain event. |
| REST adapters for the Rider app | not generated | The canvas describes no endpoints. Follow-up if you want them (stubs or real). |
| Existing classes | not converted to records, not renamed | Skill Step 5.5. Existing tests `test0`..`test3` keep their names; new tests get descriptive names. |
| Existing tests' assertions | wrap the unused `events.hasAtLeastOneEventMatching(...)` booleans in `assertThat(...).isTrue()` in `test0` and `test2` | Today these two tests assert nothing about the event. This only strengthens them; no behaviour changes. |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code | Diff |
|---|---|---|---|---|
| Rider app | Command | Rent Bike | `api/usecases/RentBikeUseCase` (`@PrimaryPort`) → `BikeStation.rent(...).by(...).forDuration(...)` | Changed: stub filled in (Q2) |
| Rider app | Command | Return Bike | `api/usecases/ReturnBikeUseCase` (`@PrimaryPort`) → `BikeStation.returnBike(bike)` | Added (aggregate method: stub filled in, Q2) |
| Rider app | Query | Available Bikes At Station | `api/queries/AvailableBikesAtStationQuery` (record), `api/queries/AvailableBikesAtStation` (result record), `api/queries/AvailableBikesAtStationQueryHandler` (`@PrimaryPort`), `api/queries/AvailableBikesReadModel` (`@SecondaryPort`), `infra/adapters/secondary/InMemoryAvailableBikesReadModel` (`@SecondaryAdapter`, projection) | Added |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Bike Returned | Event | Pricing | n/a (event) | `api/events/BikeReturned` (`IntegrationEvent`), published by `infra/adapters/secondary/IntegrationEventsPublisher`. Pricing is not in code yet, so nobody listens. |

### Ubiquitous language
| Term | Code type | Kind | Diff |
|---|---|---|---|
| Bike Station | `BikeStation`, `BikeStationId` | aggregate root, identifier | untouched (gets `@Identity id()` accessor, needed by the repository) |
| Bike | `Bike`, `BikeId`, inner `BikeInStation` | VO, identifier, entity | `BikeInStation` gets the borrower who rented it (needed for the Bike Returned payload, Q6) |
| Borrower | `Borrower`, `BorrowerId` | VO, identifier | untouched |
| Rental Duration | `RentalDuration` | VO | untouched (the 11-hour check stays in `BikeStation.rent`; boundary in Q3) |

### Business rules → aggregates
| Rule (GWT) | Invariant | Aggregate | Test | Diff |
|---|---|---|---|---|
| **Rent available bike**: given a bike station with 1 available bike, when George rents a bike for 6 hours, then the bike is rented | an available bike can be rented | `BikeStation` | `BikeStationTest#test0` | untouched (assertion strengthened) |
| **Max rental duration**: given a bike station with 1 available bike, when George rents a bike for 12 hours, then the rental is rejected | requested duration ≤ 11 h → `MaximumRentalDurationExceededException` | `BikeStation` | `BikeStationTest#test3` | untouched |
| **Return rented bike**: given a bike station where bike "bike01" was rented by George, when bike "bike01" is returned to the station, then the bike is returned and is available again | a rented bike can be returned; it becomes available → `BikeReturned` event, and renting it again succeeds | `BikeStation` | `BikeStationTest#returnRentedBike` | Added |
| **Return bike that is not rented**: given a bike station where bike "bike01" is available, when bike "bike01" is returned to the station, then the return is rejected | only a rented bike can be returned → new `BikeNotRentedException` | `BikeStation` | `BikeStationTest#returnBikeThatIsNotRented` | Added |
| **No available bike** (restored to the canvas, Q4): given a bike station with 0 available bikes, when George rents a bike for 6 hours, then the rental is rejected | the requested bike must be in the station and available → `BikeNotAvailableException` | `BikeStation` | `BikeStationTest#test1`, `#test2` | untouched (assertion in `test2` strengthened); rule added back to the canvas |

Consistency check: every GWT example was worked through against the definitions. 6 h ≤ 11 h → rented; 12 h > 11 h → rejected; the two return examples don't conflict with each other or with the definitions. No contradictions inside the canvas. The only mismatch is between the definition and the existing code (Q3).

### Rules awaiting GWT
- **Max rental duration boundary** (definition "max 11 hours" vs. code `toHours() > 11`). Suggested GWT, please confirm:
  - given a bike station with 1 available bike, when George rents a bike for 11 hours, then the bike is rented
  - given a bike station with 1 available bike, when George rents a bike for 11 hours 30 minutes, then the rental is rejected (today the code **accepts** it: `Duration.ofMinutes(690).toHours()` = 11, and 11 > 11 is false)
  - status: still awaiting a confirmed boundary example (approval kept the default "not changed"); code unchanged. Marked *not modelled yet* in `package-info.java`.
- **Return to another station** (Q5): no example in the canvas. Suggested GWT if needed: given bike "bike01" rented by George from station "Warsaw01", when "bike01" is returned to station "Warsaw02", then ... (accepted at Warsaw02 and available there? rejected?). Status: still awaiting an example; listed under "Open questions" in `package-info.java`. Today the other station rejects the return with `BikeNotRentedException`.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Rentals publishes **Bike Returned** to **Pricing** | Rentals publishes **Rental Session Ended** (start, end, location, reason) to **Settlement**; Pricing is a pure calculator called by Settlement | Partly: "Pricing reacting to *Rental Session Ended* instead of being called by Settlement" covers the direction, not the message name, the missing Settlement link, or the missing payload |
| Language: rent / return, Borrower, Rental Duration | Key language: reservation, unlock, rental session, available, withdrawn | No |
| Max rental time = a **requested** duration rejected up front (> 11 h) | Rentals **detects** max rental time and ends the session automatically; Pricing adds a penalty | No |
| No inbound relationship from Accounts | Accounts → Rentals: "is account active?" | No |
| No relationship with Fleet Maintenance | Rentals OHS/PL → Fleet Maintenance: fault detected, lock failed, withdraw / make available, reserve / unlock | No |
| Purpose omits reserve/unlock, rental sessions, station locking and withdrawing faulty bikes | Rentals owns all of these | No |

The code follows the **canvas**. The reference map and `src/main/cml/VeloCity-*.cml` are not changed (see follow-ups).

## Diff: canvas vs. existing code
- **Added**: Return Bike use case; Available Bikes At Station query (query, result, handler, read-model port, in-memory projection); `BikeNotRentedException`; integration event `api/events/BikeReturned` and its publisher; `BikePlacedInStation` domain event (Q8); in-memory `BikeStationRepo`; `RentalsContextConfig`; two return tests.
- **Changed**: `BikeStation.rent` rents the requested bike and remembers the borrower; `BikeStation.returnBike` implemented (was a stub); `BikeStation.placeNew` returns `DomainEvents` (Q8); `BikeStation` gets an `@Identity id()`; `BikeRentalEvent.BikeRented` / `BikeReturned` carry `BikeStationId` and `BikeId` (and `BikeReturned` the `Borrower`); `RentBikeUseCase` implemented and annotated `@PrimaryPort`; `BikeRentedEventHandler` gets a case for `BikePlacedInStation` (exhaustive switch); `BikeStationRepo` gets `@Repository`; `package-info.java` javadoc rewritten in the canvas structure (Markdown `///`), with the business decisions naming their tests; `test0`/`test2` assertions strengthened.
- **Removed**: nothing. In code but not in canvas: the "no available bike" rule (Q4), and the old javadoc's mention of reserving/unlocking and rental sessions (replaced by the canvas purpose).
- **Untouched**: `Bike`, `BikeId`, `BikeStationId`, `Borrower`, `BorrowerId`, `RentalDuration`, `FluentLanguage`, `BikeAlreadyInStation`, `BikeNotAvailableException`, `MaximumRentalDurationExceededException`.

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/
  package-info.java                                       M  javadoc from canvas
  api/usecases/RentBikeUseCase.java                       M  implemented, @PrimaryPort
  api/usecases/ReturnBikeUseCase.java                     A
  api/usecases/BikeStationNotFoundException.java          A  thrown when the station id is unknown
  api/queries/AvailableBikesAtStationQuery.java           A  record(String stationId)
  api/queries/AvailableBikesAtStation.java                A  record(String stationId, List<String> bikeIds)
  api/queries/AvailableBikesAtStationQueryHandler.java    A  @PrimaryPort
  api/queries/AvailableBikesReadModel.java                A  @SecondaryPort
  api/events/BikeReturned.java                            A  IntegrationEvent(String stationId, String bikeId, String borrowerId)
  domain/bikestation/BikeStation.java                     M
  domain/bikestation/BikeRentalEvent.java                 M  + BikePlacedInStation, fields on BikeRented/BikeReturned
  domain/bikestation/BikeRentedEventHandler.java          M  + case
  domain/bikestation/BikeStationRepo.java                 M  + @Repository
  domain/bikestation/BikeNotRentedException.java          A
  infra/adapters/secondary/InMemoryBikeStationRepo.java   A  @SecondaryAdapter
  infra/adapters/secondary/InMemoryAvailableBikesReadModel.java  A  @SecondaryAdapter, @EventListener projection
  infra/adapters/secondary/IntegrationEventsPublisher.java       A  @SecondaryAdapter
  infra/config/RentalsContextConfig.java                  A  beans prefixed "rentals..."
src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bikestation/
  BikeStationTest.java                                    M  + returnRentedBike, returnBikeThatIsNotRented; assertions in test0/test2
docs/bounded-contexts/velocity/rentals/
  canvas.md                                               A  copy of the source canvas (Q1) + restored "No available bike" rule (Q4)
  plan.md                                                 A  this file
```

## Stubs and follow-ups
- No ACL stubs: the only collaborator (Pricing) receives an event and isn't in code yet. Its listener belongs to Pricing when it's scaffolded.
- REST adapters for the Rider app: not generated (canvas describes no endpoints).
- No inbound command places bikes into stations; `placeNew` is only used by tests. In production the read model stays empty until such a command exists (Fleet Maintenance "make available" in the reference map).
- CML (`src/main/cml/VeloCity-*.cml`) and `velocity/velocity-context-map.md` differ from the canvas (see table above). Not changed: the map may be a workshop exercise. Offered to update; waiting for the user.
- The VeloCity `CLAUDE.md` still describes `BikeStation.returnBike` and `RentBikeUseCase` as workshop stubs; that is now out of date. Not changed (not in the approved file list); offered as a follow-up.
- The use cases, query handler, read-model projection and integration-event publisher have no dedicated tests (not in the approved plan); they are covered only by compilation and the Spring context test.

## Changelog
### 2026-11-01: updated (implemented)
- Triggered by: plan approval in the conversation; same canvas change as the entry below.
- Added: `api/usecases/ReturnBikeUseCase`, `api/usecases/BikeStationNotFoundException`, `api/queries/AvailableBikesAtStationQuery`, `AvailableBikesAtStation`, `AvailableBikesAtStationQueryHandler`, `AvailableBikesReadModel`, `api/events/BikeReturned`, `domain/bikestation/BikeNotRentedException`, `infra/adapters/secondary/InMemoryBikeStationRepo`, `InMemoryAvailableBikesReadModel`, `IntegrationEventsPublisher`, `infra/config/RentalsContextConfig`, tests `BikeStationTest#returnRentedBike` and `#returnBikeThatIsNotRented`, `docs/bounded-contexts/velocity/rentals/canvas.md`.
- Changed: `BikeStation` (rents the requested bike, remembers the borrower, `returnBike` implemented, `placeNew` returns `DomainEvents`, `@Identity id()`); `BikeRentalEvent` (+ `BikePlacedInStation`, station/bike/borrower fields); `BikeRentedEventHandler` (+ case); `BikeStationRepo` (+ `@Repository`); `RentBikeUseCase` (implemented, `@PrimaryPort`); `package-info.java` (canvas javadoc, business decisions name their tests); `BikeStationTest#test0`/`#test2` (assertions wrapped in `assertThat(...).isTrue()`); canvas copy gets the "No available bike" rule (Q4).
- Removed (confirmed by user): none. The two workshop stubs were filled in, as confirmed (Q2).
- Proposed removals (not applied): none.
- Verified: `./mvnw compile`, `./mvnw test -Dtest=BikeStationTest` (6 tests), `./mvnw test` (7 tests, Spring context starts): all pass.

### 2026-11-01: update planned (awaiting review, not applied)
- Triggered by: the Rentals canvas (`rentals-update-canvas.md`) adding **Return Bike** (command), **Available Bikes At Station** (query), **Bike Returned → Pricing** (event), and the rules *Return rented bike* and *Return bike that is not rented*.
- Added (planned): see "Diff: canvas vs. existing code".
- Changed (planned): see "Diff: canvas vs. existing code".
- Removed (confirmed by user): none.
- Proposed removals (not applied): none. The "no available bike" rule is in code but not in the canvas (Q4); proposed to keep it and add it back to the canvas.
- Files touched so far: `docs/bounded-contexts/velocity/rentals/plan.md` (this plan). No code changed.
