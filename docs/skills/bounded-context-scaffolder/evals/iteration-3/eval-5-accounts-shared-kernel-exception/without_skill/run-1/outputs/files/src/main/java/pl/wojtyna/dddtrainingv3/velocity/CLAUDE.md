# VeloCity system

VeloCity is a city bike rental service and the current exercise of the training. The repo-root `CLAUDE.md` covers the package conventions; this file adds VeloCity-specific context.

## Sources of truth

- `velocity/velocity-vision.md` (repo root) is the domain-expert role-play guide. It is **deliberately inconsistent and has gaps**, because participants are meant to discover and resolve them. Don't "fix" the vision. When the vision and the code disagree, ask instead of picking one.
- `velocity/velocity-context-map.md` is the **reference solution** for the context-mapping exercise. Its "Accepted Alternatives" section lists other valid answers. Treat it as a spoiler: don't push it on someone who is still doing the discovery exercise.
- `velocity/bounded-context-discovery-session.md` records how the bounded contexts were found, using three heuristics: pivotal events, swimlanes, and single responsibility.
- `src/main/cml/VeloCity-Bounded-Contexts.cml` (contexts only) and `VeloCity-Context-Map.cml` (with relationships) are the Context Mapper models of the reference map. `contextmap/` renders them to PNG. If you change a context or a relationship, keep the CML, `velocity-context-map.md`, and the code in sync.

## Bounded contexts

The reference map has these contexts: Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport and FleetMaintenance. It also has two external parties: the PaymentProcessor and the City, which provides the zone map. **Only `rentals` and `accounts` exist in code so far.** Add the others as new subpackages that follow the root layout.

## Accounts

- `Account` (`domain/account`) is the aggregate root, identified by the rider's borrower id. `account.block()` returns `DomainEvents` with `AccountEvent.AccountBlocked`, or throws `AccountAlreadyBlockedException` if the account is already blocked.
- **Shared kernel exception:** Accounts uses Rentals' `rentals.domain.bikestation.BorrowerId` directly in its domain, without a wrapper or ACL. The user asked for this deliberately, against the usual boundary rule. Don't "fix" it, and treat any change to `BorrowerId` as affecting both contexts.
- `BlockAccountUseCase` handles the back office's Block Account command. `infra/event/AccountsIntegrationEventsPublisher` translates the domain event into the published `api/events/AccountBlocked` (primitives only), which is meant for Rentals. Rentals doesn't consume it yet.

## Rentals

- `BikeStation` (`domain/bikestation`) is the aggregate root. It holds the bikes as a private inner `@Entity` called `BikeInStation`. `BikeStationRepo` is its `@SecondaryPort`.
- Commands use a curried fluent API built from the `FluentLanguage` interfaces: `station.rent(bike).by(borrower).forDuration(duration)` returns `DomainEvents`. Follow this style for new commands.
- Events are the sealed `BikeRentalEvent` hierarchy (`BikeRented`, `BikeReturned`). Handlers such as `BikeRentedEventHandler` use an exhaustive `switch`, so adding an event forces every handler to deal with it.
- Business rules are enforced with dedicated exceptions: `BikeAlreadyInStation`, `BikeNotAvailableException`, and `MaximumRentalDurationExceededException` (thrown when a rental is longer than 11 hours).
- Some parts are intentional workshop stubs: `BikeStation.returnBike` throws `UnsupportedOperationException`, and `RentBikeUseCase` only contains the load, execute, save, publish steps as comments.
- `rentals/package-info.java` declares the context with `@BoundedContext(name = "Rentals")` and `@Module`. The name matches the CML. Give each new VeloCity context the same kind of `package-info.java`, named after its CML `BoundedContext`.
- Tests (`src/test/.../velocity/rentals/domain/bikestation/BikeStationTest`) are written as given/when/then examples, with the scenario in `@DisplayName`, and live in the same package as the domain.
