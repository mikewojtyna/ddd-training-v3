/// # Rentals bounded context
///
/// ## Purpose
/// Owns bike availability at stations, renting and returning bikes, and the maximum rental time,
/// so riders can always find and return a bike and bikes are never held forever.
///
/// ## Strategic classification
/// - Domain: core
/// - Business model: revenue
/// - Evolution: custom built
///
/// ## Domain roles
/// - Execution Model: performs and tracks the rental job.
///
/// ## Ubiquitous language
/// - **Bike Station**: a physical dock holding bikes.
/// - **Bike**: a rentable bicycle identified by its id.
/// - **Borrower**: the rider renting a bike.
/// - **Rental Duration**: requested length of a rental; max 11 hours.
///
/// ## Business decisions
/// - **Rent available bike**: given a bike station with 1 available bike, when George rents a bike for 6 hours,
///   then the bike is rented. Enforced by `BikeStation`, see `BikeStationTest#test0`.
/// - **Max rental duration**: given a bike station with 1 available bike, when George rents a bike for 12 hours,
///   then the rental is rejected. Enforced by `BikeStation`, see `BikeStationTest#test3`.
/// - **Max rental duration boundary** (*not modelled yet*): whether a rental of 11 hours 30 minutes is rejected.
///   Today the check compares whole hours, so it is accepted. Awaiting a strict Given/When/Then example, see the plan.
/// - **Return rented bike**: given a bike station where bike "bike01" was rented by George, when bike "bike01" is
///   returned to the station, then the bike is returned and is available again. Enforced by `BikeStation`,
///   see `BikeStationTest#returnRentedBike`.
/// - **Return bike that is not rented**: given a bike station where bike "bike01" is available, when bike "bike01"
///   is returned to the station, then the return is rejected. Enforced by `BikeStation`,
///   see `BikeStationTest#returnBikeThatIsNotRented`.
/// - **No available bike**: given a bike station with 0 available bikes, when George rents a bike for 6 hours,
///   then the rental is rejected. Enforced by `BikeStation`, see `BikeStationTest#test1` and `#test2`.
///
/// ## Open questions
/// - Returning a bike to a different station than the one it was rented from is not modelled: that station
///   rejects the return. Awaiting a Given/When/Then example, see the plan.
///
/// Source canvas: `docs/bounded-contexts/velocity/rentals/canvas.md`
@BoundedContext(name = "Rentals")
@Module
package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
