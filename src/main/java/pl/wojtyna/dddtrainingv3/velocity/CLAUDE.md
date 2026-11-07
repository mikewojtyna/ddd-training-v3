# VeloCity system

VeloCity is a city bike rental service and the current exercise of the training. The repo-root `CLAUDE.md` covers the package conventions; this file adds VeloCity-specific context.

## Sources of truth

- `velocity/velocity-vision.md` (repo root) is the domain-expert role-play guide. It is **deliberately inconsistent and has gaps**, because participants are meant to discover and resolve them. Don't "fix" the vision. When the vision and the code disagree, ask instead of picking one.
- `velocity/velocity-context-map.md` is the **reference solution** for the context-mapping exercise. Its "Accepted Alternatives" section lists other valid answers. Treat it as a spoiler: don't push it on someone who is still doing the discovery exercise.
- `velocity/bounded-context-discovery-session.md` records how the bounded contexts were found, using three heuristics: pivotal events, swimlanes, and single responsibility.
- `src/main/cml/VeloCity-Bounded-Contexts.cml` (contexts only) and `VeloCity-Context-Map.cml` (with relationships) are the Context Mapper models of the reference map. `contextmap/` renders them to PNG. If you change a context or a relationship, keep the CML, `velocity-context-map.md`, and the code in sync.

## Bounded contexts

The reference map has these contexts: Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport and FleetMaintenance. It also has two external parties: the PaymentProcessor and the City, which provides the zone map. **Only `rentals` exists in code so far.** Add the others as new subpackages that follow the root layout.

## Rentals

- `BikeStation` (`domain/bikestation`) is an aggregate root. It holds the bikes as a private inner `@Entity` called `BikeInStation` (availability, reservation window, holder). `BikeStationRepo` is its `@SecondaryPort`.
- `Borrowing` (`domain/borrowing`) is the second aggregate root: what one borrower currently holds across all stations ("one bike at a time"): at most one reservation (R2), no reserving while riding (R7), a holder rents only the reserved bike (R8), only eligible borrowers reserve (R6). `BorrowingRepository` is its port. Reserving and renting change both aggregates in one use case.
- Reservations come from `docs/specs/rider-reserves-bike-at-station.md` (plan `docs/plans/rider-reserves-bike-at-station.md`). The 15-minute `ReservationWindow` includes its end time; after it a reservation stops counting.
- Commands use a curried fluent API built from the `FluentLanguage` interfaces: `station.rent(bike).by(borrower).forDuration(duration).at(now)` and `station.reserve(bike).by(borrower).at(now)` return `DomainEvents`. Time is always passed in; use cases take it from the `rentalsClock` bean. Follow this style for new commands.
- Events are the sealed `BikeRentalEvent` hierarchy (`BikeRented` with the bike and whether it came from a reservation, `BikeReturned`, `BikeReserved`). Handlers such as `BikeRentedEventHandler` use an exhaustive `switch`, so adding an event forces every handler to deal with it.
- Business rules are enforced with dedicated exceptions: `BikeAlreadyInStation`, `BikeNotAvailableException`, and `MaximumRentalDurationExceededException` (thrown when a rental is longer than 11 hours).
- `BikeStation.returnBike` is still an intentional workshop stub (`UnsupportedOperationException`). `RentBikeUseCase` is implemented (it was a comment-only stub until the reservation intent). Rider Accounts' eligibility adapter (`rideraccountsctxacl`) is a stub; tests use a fake.
- `rentals/package-info.java` declares the context with `@BoundedContext(name = "Rentals")` and `@Module`. The name matches the CML. Give each new VeloCity context the same kind of `package-info.java`, named after its CML `BoundedContext`.
- Tests (`src/test/.../velocity/rentals/domain/bikestation/BikeStationTest`) are written as given/when/then examples, with the scenario in `@DisplayName`, and live in the same package as the domain.
