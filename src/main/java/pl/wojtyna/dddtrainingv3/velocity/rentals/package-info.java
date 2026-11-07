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
/// - **R1 Reserve without being at the station** [E1.1, E1.2]: *being built with `ddd-tactical-building-blocks`*.
/// - **R2 One active reservation per borrower** [E2.1]: *being built with `ddd-tactical-building-blocks`*.
/// - **R3 A reserved bike is held for its borrower**, up to and including the end of its window [E3.1, E3.2]:
///   *being built with `ddd-tactical-building-blocks`*.
/// - **R4 Reserved bikes aren't offered to others** [E4.1]: *being built with the `AvailableBikesAtStation` query*.
/// - **R5 Withdrawn bikes can't be reserved** [E5.1]: *being built with `ddd-tactical-building-blocks`*.
/// - **R6 Only eligible borrowers reserve** [E6.1]: *being built with `ddd-tactical-building-blocks`*.
/// - **R7 No reserving while riding** [E7.1]: *being built with `ddd-tactical-building-blocks`*.
/// - **R8 A holder rents only the reserved bike, and renting it ends the reservation** [E8.1, E8.2]:
///   *being built with `ddd-tactical-building-blocks`*.
/// - **A rental can't be longer than 11 hours**: enforced, see `BikeStationTest`.
@BoundedContext(name = "Rentals")
@Module
package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
