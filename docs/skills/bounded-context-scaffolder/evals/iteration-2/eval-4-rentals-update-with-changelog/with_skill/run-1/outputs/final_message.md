Rentals is updated to match the canvas. All builds pass: `./mvnw compile`, `./mvnw test -Dtest=BikeStationTest` (6 tests) and `./mvnw test` (7 tests, and the Spring context starts with the new `RentalsContextConfig`). Nothing is committed.

**Where things are**
- Plan: `docs/bounded-contexts/velocity/rentals/plan.md`. Its status is now `implemented`. The approval is recorded under "Review notes", and there's a new dated changelog entry listing every file touched. The older "planned" entry is kept as history.
- Canvas: `docs/bounded-contexts/velocity/rentals/canvas.md`. It's copied from the eval file (Q1), with the "No available bike" rule added back (Q4).
- Module: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/`. Tests: `src/test/.../velocity/rentals/domain/bikestation/BikeStationTest.java`.

**What changed, following the approved plan and your defaults**
- **Stubs filled in (Q2):**
  - `BikeStation.returnBike` returns a rented bike and emits `BikeReturned`. It rejects a bike that isn't rented with the new `BikeNotRentedException`. That includes a bike the station doesn't know, which is how a return to a different station is handled (Q5).
  - `RentBikeUseCase` now runs the load, execute, save, publish steps and has `@PrimaryPort`. The original step comments are still there.
- **Return Bike:** a new `ReturnBikeUseCase`, written in the same style. Both use cases throw `BikeStationNotFoundException` for an unknown station.
- **`BikeStation` changes:**
  - `rent(bike)` now rents the bike you ask for (before, it took any available bike) and remembers who rented it.
  - `placeNew` returns `DomainEvents` with a new `BikePlacedInStation` event (Q8).
  - It has an `@Identity id()`.
  - The curried fluent API is unchanged.
- **Events:** `BikeRented` and `BikeReturned` now carry the station id, the bike id and the borrower. `BikeRentedEventHandler` handles the new event in its exhaustive switch.
- **Available Bikes At Station query:**
  - Query, result and handler, plus the `AvailableBikesReadModel` port.
  - The result is the station id plus a list of bike ids (Q7).
  - The read model is in memory. It's built from the `BikePlacedInStation`, `BikeRented` and `BikeReturned` events.
- **Bike Returned to Pricing:** a new integration event `api/events/BikeReturned` carrying three strings: station id, bike id and borrower id (Q6). `IntegrationEventsPublisher` creates it from the domain event. No ACL is needed, because it's an outbound event.
- **Wiring:** `InMemoryBikeStationRepo`, and `BikeStationRepo` now has `@Repository`. `RentalsContextConfig` names all its beans with a `rentals...` prefix.
- **Tests:**
  - New: `returnRentedBike` and `returnBikeThatIsNotRented`.
  - Existing: `test0` and `test2` used to check the event without asserting the result. They now wrap it in `assertThat(...).isTrue()`.
  - Nothing was deleted or renamed. No existing class was converted to a record.
- **`package-info.java`:** rewritten from the canvas. Each business decision that's enforced names its test.

**Still open**
- **Q3, the 11-hour boundary:** unchanged. 11 h 30 min is still accepted, because the check compares whole hours. The javadoc and the plan mark it *not modelled yet*. Please confirm the suggested GWT example if you want it fixed.
- **Q5, returning to another station:** that station rejects the return. If cross-station returns should work, give me a GWT example.
- **Rental times for Pricing:** the Bike Returned event has no start or end time, because Rentals doesn't track time yet.
- **Placing bikes in stations:** no inbound command does this. In production, the repository and the read model stay empty until one exists (in the reference map, Fleet Maintenance's "make available").
- **No listener for Bike Returned:** Pricing isn't in the code yet.
- **Untested code:** there are no dedicated tests for the use cases, the query handler, the projection or the publisher. The plan didn't include them, so they're covered only by compilation and the Spring context test. I can add them.
- **Stale docs:** the VeloCity `CLAUDE.md` still calls `returnBike` and `RentBikeUseCase` workshop stubs. I can update it.

**Differences from the reference context map:** the plan's table is unchanged.
- The canvas has Rentals publish **Bike Returned** to **Pricing**. The map has **Rental Session Ended** going to Settlement. "Accepted Alternatives" covers the direction only.
- The canvas also differs from the map on language, on how max rental time is handled, and on the Accounts and Fleet Maintenance relationships.

`src/main/cml/VeloCity-*.cml` and `velocity/velocity-context-map.md` don't match this canvas now. I didn't touch them, because they may be the workshop exercise. Should I bring them in sync?
