/// # Rentals bounded context
///
/// ## Purpose
/// Gets a bike into someone's hands in seconds and makes sure every bike is used by one person at a time: it knows
/// which bikes are available, reserves and unlocks them for riders and technicians, runs rental sessions from unlock
/// to return, and makes sure every session ends: by a return at a station, a free-floating return, a failed lock, or
/// the maximum rental time. It speaks of bikes as *available* or *withdrawn*, never *broken*: why a bike is withdrawn
/// is its clients' business.
///
/// ## Strategic classification
/// - Domain: core
/// - Business model: revenue
/// - Evolution: custom built
///
/// ## Domain roles
/// - Execution Model: runs the reservation → unlock → session → return lifecycle.
/// - Enforcer: guarantees one bike, one active holder.
///
/// ## Ubiquitous language
/// - **Fleet**: all bikes Rentals can lend or that wait for repair; a bike joins it once, when a fleet manager adds it
///   at a station.
/// - **Bike**: a vehicle Rentals can lend; it is *available* or *withdrawn*, and at the same time free, *reserved* or
///   *unlocked*.
/// - **Available**: may be reserved by a rider.
/// - **Withdrawn**: not offered to riders; only a technician may reserve it. Rentals withdraws a bike itself on a fault
///   it detects (failed lock, fault reported at return, fault signalled by the bike, not returned) or when a client asks.
/// - **Holder**: whoever reserved or unlocked a bike: a *rider* or a *technician*.
/// - **Reservation**: a short exclusive hold on one bike for one holder: 2 minutes for a rider (DE-10), 30 minutes for a
///   technician (DE-11), holding up to and including its end (DE-81).
/// - **Unlock**: opening the bike's lock for its reservation holder. A rider's unlock starts a rental session.
/// - **Rental session**: a rider's ride, from unlock until the session ends.
/// - **End reason**: *station return*, *free-floating return*, *lock failure*, *maximum rental time exceeded*.
/// - **Station**: an official dock where bikes are locked; Rentals knows where each station is (`Central`,
///   `Old Town`, `Riverside`).
/// - **Maximum rental time**: 12 hours (DE-20).
/// - **Near**: within 50 metres (DE-12).
///
/// ## Business decisions
/// Enforced by the aggregate `Bike` (`domain/bike`) and the factory `Fleet`; one test class per area, in the domain
/// package. Rentals asks Accounts for eligibility through the port `RentalEligibility` and opens and closes locks
/// through the port `BikeLocks`; both answers are passed into the bike, which decides.
/// - **A fleet manager adds a new bike to the fleet at a station; only once; only at a station** (DE-18):
///   `FleetTest#newBikeAddedAtStationIsAvailableThere`, `#addingSameBikeAgainIsRejected`, `#addingBikeOutsideAStationIsRejected`.
/// - **An eligible rider within 50 m reserves an available, free bike for 2 minutes** (DE-10, DE-12, DE-13, DE-14):
///   `BikeReservationTest#eligibleRiderNearbyReservesAvailableBikeForTwoMinutes`, `#riderFarAwayIsRejected`,
///   `#riderExactlyFiftyMetresAwayReserves`, `#ineligibleRiderIsRejected`, `#riderHoldingAReservationCannotReserveAnotherBike`,
///   `#riderInRunningRideCannotReserve`, `#bikeReservedForSomeoneElseIsRejected`, `#withdrawnBikeIsRejected`.
/// - **The holder of a reservation that still holds unlocks; a rider's unlock starts a session; a failed lock releases
///   the reservation; an ineligible rider's unlock changes nothing** (DE-15, DE-80, DE-81): `BikeUnlockTest#holderUnlocksAndSessionStarts`,
///   `#unlockAtExactEndOfReservationStartsSession`, `#failedUnlockReleasesReservationAndBikeIsAvailable`,
///   `#afterFailedUnlockRiderReservesAnotherBike`, `#otherRiderCannotUnlockReservedBike`, `#unlockWithoutReservationIsRejected`,
///   `#unlockAfterReservationPeriodIsRejected`, `#riderBlockedDuringReservationCannotUnlock`.
/// - **A reservation not used in its period expires; the bike's availability doesn't change** (DE-81):
///   `ReservationExpiryTest` (rider and technician examples).
/// - **A station return ends the session at the named station; a failed lock ends it as a lock failure and withdraws
///   the bike; a reported fault withdraws it with normal fees** (DE-22, DE-24): `StationReturnTest`.
/// - **A free-floating return ends the ride where the rider is; a failed lock is a lock failure wherever it is**
///   (DE-23): `FreeFloatingReturnTest`.
/// - **A session longer than 12 hours ends at the 12-hour mark and the bike is withdrawn as not returned** (DE-20,
///   DE-21, DE-82): `OverdueSessionTest`.
/// - **A fault signalled by the bike withdraws it at once outside a ride, at the session end during one, never twice**
///   (DE-25, DE-86; one withdrawal per session end, plan decision D6): `FaultSignalTest`.
/// - **A client's withdrawal cancels a reservation, lets a ride go on, and changes nothing on a withdrawn bike**
///   (DE-86): `BikeWithdrawalTest`.
/// - **Technicians reserve only withdrawn bikes, without a position, several at a time; their unlock never starts a
///   session; a withdrawn bike unlocked for a technician can be made available at a station or a position** (DE-11,
///   DE-16, DE-26, DE-91, DE-92): `TechnicianServiceTest`.
///
/// ## Assumptions
/// - DE-14 as revised by DE-80: eligibility is checked at reservation and again at a rider's unlock.
/// - DE-82: an overdue session ends at the 12-hour mark, whenever it is noticed.
/// - DE-25: a fault signalled during a ride withdraws the bike when the session ends.
/// - DE-22: the client-reported station and position are trusted.
/// - DE-26: the 50 m rule applies to riders only.
/// - The rider is "told why" a session ended through the answer to their return command, which states the end reason.
/// - DE-86: a bike withdrawn while a rider rides it keeps the ride going; it simply isn't available after the return.
///
/// ## Verification metrics
/// - Seconds from reservation to session start.
/// - Share of reservations that expire or fail to unlock.
///
/// ## Open questions
/// - Should repeated failed unlocks of the same bike withdraw it?
/// - Should a rider be warned before the maximum rental time is reached?
///
/// Source canvas: `docs/bounded-contexts/velocity/rentals/canvas.md`
@BoundedContext(name = "Rentals")
@Module
package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
