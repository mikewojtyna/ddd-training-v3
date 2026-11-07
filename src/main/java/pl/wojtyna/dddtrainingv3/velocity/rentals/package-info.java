/// Rentals bounded context. Answers **who has which bike, and when?** Lets eligible borrowers reserve, unlock, ride and
/// return bikes, and records each rental session. Implemented as a single module.
///
/// Canvas: `docs/bounded-contexts/velocity/rentals/canvas.md`. Context plan: `docs/bounded-contexts/velocity/rentals/plan.md`.
/// The discovery CML calls this context `Rental`.
///
/// ## Ubiquitous language
/// - **Reservation**: a borrower's hold on one specific bike at a station, made without being at the station.
/// - **Reservation window**: 15 minutes from the moment of reserving, including the end time. Once it has passed,
///   the reservation stops counting.
/// - **Borrower**: the rider holding or renting a bike.
/// - **Eligible borrower**: a borrower whose account Rider Accounts reports as activated.
/// - **Withdrawn bike**: a bike Fleet Maintenance has taken out of service; it isn't available.
///
/// ## Collaborators
/// - **Rider Accounts** (upstream): "can this rider rent?", through the `rideraccountsctxacl` ACL (stub: not in code yet).
/// - **Fleet Maintenance** (downstream): commands *Withdraw bike* through `api.usecases.WithdrawBike`.
///
/// ## Business decisions
/// From `docs/specs/rider-reserves-bike-at-station.md` (spec IDs in brackets):
/// - **A bike is rented exactly as requested**: given B-17 and B-22 are available at Central, when George rents B-17,
///   then B-17 is rented and B-22 stays available. `BikeStation` invariant, see
///   `BikeStationTest#rentsExactlyTheRequestedBike`.
/// - **R1 Reserve without being at the station** [E1.1, E1.2]: given B-17 is available at Central, when Anna reserves
///   it at 10:00, then it's reserved for her until 10:15 (Bike Reserved); a rented bike can't be reserved. `BikeStation`
///   invariant, see `BikeStationTest#reservesAnAvailableBikeForFifteenMinutes`, `#rejectsReservingARentedBike`.
/// - **R2 One active reservation per borrower** [E2.1]: given Anna holds B-17 at Central until 10:15, when she reserves
///   B-22 at Riverside at 10:05, then rejected. `Borrowing` invariant, see `BorrowingTest#rejectsASecondActiveReservation`.
/// - **R3 A reserved bike is held for its borrower**, up to and including the end of its window [E3.1, E3.2]:
///   given B-17 is reserved for Anna until 10:15, when Ben reserves it at 10:05 or rents it at 10:15, then rejected;
///   at 10:16 the reservation no longer counts (plan D9). `BikeStation` invariant, see
///   `BikeStationTest#rejectsReservingABikeReservedForSomeoneElse`,
///   `#rejectsRentingABikeReservedForSomeoneElseUpToTheEndOfTheWindow`, `#aReservationStopsCountingAfterItsWindow`.
/// - **R4 Reserved bikes aren't offered to others** [E4.1]: given B-17 is reserved for Anna and B-22 is available, when
///   Ben asks which bikes are available at Central, then he sees only B-22. `BikeStation#availableBikesAt`, served by
///   `AvailableBikesAtStationHandler`; see `BikeStationTest#aReservedBikeIsNotOfferedToOthers`.
/// - **R5 Withdrawn bikes can't be reserved** [E5.1]: given B-31 was withdrawn, when Anna reserves it, then rejected.
///   `BikeStation` invariant, see `BikeStationTest#rejectsReservingAWithdrawnBike`.
/// - **R6 Only eligible borrowers reserve** [E6.1]: given Carl hasn't finished onboarding, when he reserves B-17, then
///   rejected. `Borrowing` invariant; the eligibility is a command argument the use case gets from Rider Accounts through
///   `BorrowerEligibility`. See `BorrowingTest#onlyAnEligibleBorrowerCanReserve`.
/// - **R7 No reserving while riding** [E7.1]: given Anna rented B-05 at 09:40, when she reserves B-17 at 10:00, then
///   rejected. `Borrowing` invariant, see `BorrowingTest#rejectsReservingWhileRiding`.
/// - **R8 A holder rents only the reserved bike, and renting it ends the reservation** [E8.1, E8.2]: given Anna holds
///   B-17 until 10:15, when she rents B-22 at 10:05, then rejected; when she rents B-17 at 10:10, then the rental starts
///   from her reservation and the reservation ends. `Borrowing` invariant (R8a) and `BikeStation` invariant (holder
///   rents), see `BorrowingTest#aHolderCanRentOnlyTheReservedBike`, `#aHolderCanRentTheReservedBike`,
///   `BikeStationTest#theHolderRentsTheReservedBikeAndTheReservationEnds`.
/// - **A rental can't be longer than 11 hours**: enforced, see `BikeStationTest`.
@BoundedContext(name = "Rentals")
@Module
package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
