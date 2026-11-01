I updated the Rentals bounded context to match the new canvas. `./mvnw test` passes: 10 tests, 0 failures.

## What changed

**New: Return Bike command (Rider app)**
- `BikeStation.returnBike(bike)` used to throw `UnsupportedOperationException`. It now makes the bike available again and emits `BikeReturned(station, bike, by)`.
- Returning a bike that isn't rented throws the new `BikeNotRentedException`.
- New `api/usecases/ReturnBikeUseCase` (`@PrimaryPort`) loads the station, calls `returnBike`, saves, and publishes the events. If the station doesn't exist it throws the new `BikeStationNotFoundException`.

**New: Available Bikes At Station query (Rider app)**
- `BikeStation.availableBikes()` was added.
- New `api/queries/AvailableBikesAtStationQuery` (`@PrimaryPort`) returns an `AvailableBikesAtStation(stationId, bikeIds)` read model.

**New: Bike Returned event to Pricing**
- New integration event `api/events/BikeReturned(stationId, bikeId, borrowerId)`.
- `infra/adapters/secondary/events/RentalsIntegrationEventsPublisher` (`@SecondaryAdapter`) turns the domain `BikeReturned` into this event.

**Changed: existing behaviour**
- `rent(bike)` now rents the bike you ask for. Before, it took any available bike and ignored the argument, so the "bike01 was rented by George, then returned" scenario wouldn't have worked. The bike now remembers who rented it, so `BikeReturned` can say who returned it.
- Max rental duration is now a precise check: `RentalDuration.MAXIMUM = 11h`, enforced by `exceedsMaximum()`. The old `toHours() > 11` check let rentals of up to 11h59m through. `RentalDuration` is now annotated `@ValueObject`.
- `BikeRented` now also carries the station and the bike. `BikeReturned` carries station, bike and borrower; before, it had no fields.

**Infrastructure**
- `infra/config/RentalsContextConfig` wires the repository, `ReturnBikeUseCase`, the query and the event translator.
- `infra/adapters/secondary/repository/InMemoryBikeStationRepo` is the repository.

**Docs**
- The `package-info.java` Javadoc now uses the canvas purpose and classification: core domain, revenue, custom built, execution model.
- `velocity/CLAUDE.md` is updated so it no longer calls `returnBike` a stub.

**Tests (given/when/then, scenario in `@DisplayName`)**
- `BikeStationTest` has 4 new tests:
  - return a rented bike
  - return a bike that isn't rented (rejected)
  - 11h01m is rejected
  - renting one of two bikes leaves only the other one available
- New `ReturnBikeUseCaseTest` covers the use case, the event it publishes, and the query, using the in-memory repo.

## Questions (I made a call on each, since I couldn't ask)

1. **Rentals -> Pricing conflicts with the reference context map.** The CML and `velocity-context-map.md` have no Rentals -> Pricing link: Settlement calls Pricing, and "Pricing reacting" is only listed as an accepted alternative. I built the event as the canvas says but **did not change the CML or the context-map doc**, because they are the reference solution. Should I add `Rentals [U,PL]->[D,CF] Pricing : BikeReturned` and switch the map to that alternative?
2. **Workshop stubs.** `returnBike` was an intentional exercise stub. I implemented it because the canvas now needs Return Bike. `RentBikeUseCase` is still the commented stub, and the only Spring bean added is `ReturnBikeUseCase`. Should I implement `RentBikeUseCase` too, or keep it for participants?
3. **Returning to a different station.** Returning a bike this station doesn't know about is rejected with `BikeNotRentedException`. In reality a rider might return a bike to any station. Is that in scope?
4. **Contents of Bike Returned.** I chose station id, bike id and borrower id. Pricing probably also needs the rental start time and requested duration. Should those be added, which means tracking when the rental started?
5. **Is exactly 11 hours allowed?** I took "max 11 hours" to mean 11h is allowed and anything longer is rejected.
6. **Canvas language vs. the reference map.** The canvas drops the reference map's terms (reservation, unlock, rental session, withdrawn). I kept the code to the canvas's language. Should the CML vision statement be brought in line?
