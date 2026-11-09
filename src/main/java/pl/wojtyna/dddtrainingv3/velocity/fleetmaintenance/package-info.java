/// # FleetMaintenance bounded context
///
/// ## Purpose
/// Keeps the fleet healthy: knows every broken bike, whatever reported it, lets technicians pick broken bikes up and
/// repair them, and decides when a repaired bike goes back into circulation. Maintenance is never billed.
///
/// ## Strategic classification
/// - Domain: supporting
/// - Business model: cost reduction
/// - Evolution: custom built
///
/// ## Domain roles
/// - Enforcer: enables Rentals to offer only working bikes.
/// - Execution Model: runs each broken bike through maintenance.
///
/// ## Ubiquitous language
/// - **Broken bike**: a bike reported faulty; riders can't rent it until it's fixed (`BikeCondition.BROKEN`).
/// - **Fault report**: a report that a bike is broken: from Rentals (it withdrew the bike), from a fleet-issue
///   incident, or from a technician (`FaultReport`, `FaultSource`).
/// - **Incident link**: a fleet-issue incident about the broken bike, resolved when the bike is fixed (`IncidentId`).
/// - **Technician**: a VeloCity employee who reserves and unlocks broken bikes to repair them (`TechnicianId`).
/// - **Maintenance**: the work after a technician's unlock; never billed (`Maintenance`).
/// - **Maintenance started**: the moment a technician's unlock of a broken bike is reported (`MaintenanceStarted`).
/// - **Bike fixed / back in circulation**: the technician returned the bike to circulation after maintenance; there
///   is no separate "mark fixed" step (`BikeFixed`, `BikeCondition.IN_CIRCULATION`).
///
/// ## Business decisions
/// All enforced by the `Bike` aggregate (`domain/bike`).
/// - **A bike withdrawn by Rentals is registered broken**: given B-17 isn't broken, when Rentals reports it withdrawn
///   with reason *lock failed* (or *not returned*, DE-21), then B-17 is broken with that fault source. See
///   `FaultReportTest#bikeWithdrawnForFailedLockBecomesBroken`, `#bikeNotReturnedBecomesBroken`. A withdrawal with
///   reason *requested* (asked for by Fleet Maintenance itself) is not a fault report (`BikeWithdrawnListener`).
/// - **A fleet-issue incident makes the bike broken and withdraws it** (DE-52): see
///   `FaultReportTest#fleetIssueMakesBikeBrokenAndRequestsWithdrawal`.
/// - **A technician's fault report makes the bike broken and withdraws it** (DE-62): see
///   `FaultReportTest#technicianFaultReportMakesBikeBrokenAndRequestsWithdrawal`.
/// - **A duplicate fault report changes nothing; an incident is only linked**: see
///   `FaultReportTest#technicianFaultReportOnBrokenBikeChangesNothing`, `#fleetIssueOnBrokenBikeOnlyLinksIncident`,
///   `#fixedBikeCanBreakAgain`.
/// - **Only a broken bike can be reserved by a technician** (DE-16); Rentals holds it and says until when (DE-11,
///   DE-26, DE-92): see `TechnicianReservationTest#technicianReservesBrokenBikeUntilRentalsSays`,
///   `#technicianCannotReserveBikeThatIsNotBroken`, `#secondTechnicianCannotReserveReservedBike`.
/// - **A technician's unlock starts maintenance**: see `MaintenanceTest#technicianUnlockStartsMaintenance`,
///   `#unlockOfBrokenBikeIsForwardedToRentals`, `#unlockOfBikeThatIsNotBrokenIsRejected`,
///   `#repeatedUnlockReportChangesNothing`.
/// - **An expired reservation or a failed unlock leaves the bike broken**, reservable by any technician: see
///   `TechnicianReservationTest#expiredReservationLeavesBikeBroken`,
///   `#releasingAnotherTechniciansReservationChangesNothing`, `MaintenanceTest#failedUnlockLeavesBikeBrokenAndUnreserved`.
/// - **A bike returns to circulation only after maintenance has started**: see
///   `CirculationReturnTest#returnBeforeMaintenanceStartedIsRejected`, `#returnOfBikeThatIsNotBrokenIsRejected`.
/// - **Returning a bike under maintenance fixes it**, by any technician, at a station or a position (DE-60, DE-91),
///   once Rentals made it available; its linked incidents are resolved (DE-55): see
///   `CirculationReturnTest#returnAtStationFixesBike`, `#returnAtPositionFixesBike`, `#anyTechnicianMayReturnBike`,
///   `#fixedBikeNamesItsLinkedIncidents`, `#bikeIsNotFixedWhenRentalsRefuses`.
///
/// ## Integration
/// - Technician app: REST resources under `/fleet-maintenance` (integration-contracts.md).
/// - Consumes `rentals.api.events.BikeWithdrawn` (all reasons except `REQUESTED`), `BikeUnlocked`, `ReservationExpired`,
///   `UnlockFailed` (technicians only) and `customersupport.api.events.FleetIssueReported`, through the ACL listeners
///   in `infra/adapters/primary/rentalsctxacl` and `customersupportctxacl`.
/// - Calls Rentals' OHS (Withdraw Bike, Reserve/Unlock Bike for service, Make Bike Available, Bike Availability) and
///   Customer Support's OHS (Resolve Fleet Issue) through the ACLs in `infra/adapters/secondary/rentalsctxacl` and
///   `customersupportctxacl`.
/// - Publishes no integration events yet (Bike Reported Broken, Maintenance Started, Bike Fixed stay inside).
///
/// ## Assumptions
/// - DE-52: fleet-issue incidents reach Fleet Maintenance.
/// - DE-21: a bike not returned after the maximum rental time is treated as broken.
/// - Rentals owns reserve/unlock; Fleet Maintenance never locks or unlocks bikes itself.
/// - DE-91: any technician may return a bike under maintenance, at the station or position they name.
/// - DE-92: a technician may hold several reservations; a fault report on a bike in a ride is accepted and the ride
///   goes on.
/// - A withdrawal that Fleet Maintenance asked for itself (reason *requested*) is not a new fault report.
///
/// ## Verification metrics
/// - Time from "reported broken" to "back in circulation".
/// - Share of bikes reported broken again within 7 days of returning to circulation.
///
/// ## Open questions
/// - Can a bike turn out to be unrepairable (retired from the fleet)? (DE-61: out of scope.)
/// - Is there a repair priority (e.g. bikes at busy stations first)?
///
/// Source canvas: `docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`
@BoundedContext(name = "FleetMaintenance")
@Module
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
