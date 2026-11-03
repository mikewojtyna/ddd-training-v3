/// # FleetMaintenance bounded context
///
/// ## Purpose
/// Keeps the fleet healthy: makes sure a broken bike is fixed as quickly as possible and decides
/// when it goes back into circulation.
///
/// ## Strategic classification
/// - Domain: supporting
/// - Business model: cost reduction
/// - Evolution: custom built
///
/// ## Domain roles
/// - Enforcer: enables Rentals to offer only working bikes
///
/// ## Ubiquitous language
/// - **Bike**: a physical vehicle in the VeloCity fleet.
/// - **Broken**: a bike that has been reported faulty; riders can't rent it until it's fixed.
/// - **Fault Report**: a report that a bike is broken, from a rider at return, from a support incident, from the
///   bike itself, or from a technician.
/// - **Technician**: a VeloCity employee who reserves and unlocks bikes in order to repair them.
/// - **Maintenance**: the work a technician does on a broken bike after unlocking it; never billed.
/// - **Maintenance Started**: the moment a technician unlocks a broken bike.
/// - **Bike Fixed**: the moment a technician returns a bike to circulation after maintenance. There is no separate
///   "mark fixed" step: returning it means it is fixed.
/// - **Back in Circulation**: the bike is at a station or in a drop zone and available to riders again.
///
/// ## Business decisions
/// - **A bike returns to circulation only after maintenance has started** (R1): given bike B-17 is broken and
///   maintenance on it has not started, when a technician returns B-17 to circulation, then the request is rejected
///   and B-17 stays withdrawn. Enforced by `Bike`, see `BikeTest#returnWithoutMaintenanceIsRejected`.
/// - **Returning a bike under maintenance fixes it** (R2): given bike B-17 is broken and maintenance on it has
///   started, when a technician returns B-17 to circulation, then *Bike Fixed* is recorded for B-17 and Rentals is
///   asked to make B-17 available. Enforced by `Bike` and `ReturnFixedBikeToCirculationPolicy`, see
///   `BikeTest#returningBikeUnderMaintenanceFixesIt` and
///   `ReturnFixedBikeToCirculationPolicyTest#fixedBikeIsPutBackIntoCirculation`.
/// - **Maintenance starts only on a broken bike** (R3): given bike B-17 is not broken, when a technician's unlock of
///   B-17 is reported, then the unlock is ignored: nothing is recorded and no *Maintenance Started* is published.
///   Enforced by `Bike`, see `BikeTest#technicianUnlockOfBikeThatIsNotBrokenIsIgnored`.
/// - **A broken bike waits after an expired reservation** (R4): given bike B-17 is broken and reserved by technician
///   T-1, when the reservation expires before T-1 unlocks it, then B-17 is still broken, is not offered to riders, and
///   can be reserved by any technician. Reservations are Rentals' state; Fleet Maintenance enforces its side: any
///   technician can start maintenance and nothing puts the bike back. See
///   `BikeTest#anyTechnicianCanStartMaintenanceAfterExpiredReservation`.
/// - **A duplicate fault report is ignored** (R5): given bike B-17 is already broken, when a second fault report for
///   B-17 arrives, then nothing changes and nothing is recorded. Enforced by `Bike`, see
///   `BikeTest#duplicateFaultReportIsIgnored`.
/// - **Customer Support is told about every fix** (R6): given B-17 under maintenance, when a technician returns B-17
///   to circulation, then Customer Support is told B-17 is fixed. Enforced by `NotifySupportOfFixedBikePolicy`, see
///   `NotifySupportOfFixedBikePolicyTest#supportIsToldBikeIsFixed`.
/// - **A fault report makes a bike broken** (U1, from the definition of *Broken*): given B-17 in circulation, when a
///   fault is reported, then B-17 is broken. See `BikeTest#faultReportMakesBikeBroken`.
/// - **A technician's unlock of a broken bike starts maintenance** (U2, from the definition of *Maintenance
///   Started*): given B-17 broken, when technician T-1's unlock of B-17 is reported, then *Maintenance Started* by
///   T-1 is recorded. See `BikeTest#technicianUnlockOfBrokenBikeStartsMaintenance`.
/// - **Withdrawing a broken bike in Rentals** (*not modelled yet*): when *Withdraw Bike* is sent is open (Q3).
///   Awaiting a strict Given/When/Then example, see the plan.
/// - **Fault report during maintenance** (*not modelled yet*, Q5): currently ignored, because a bike under
///   maintenance is still broken. Awaiting confirmation, see the plan.
/// - **Second technician unlock during maintenance** (*not modelled yet*, Q6): currently ignored. See the plan.
/// - **Who may return the bike** (*not modelled yet*, Q7): currently any technician. See the plan.
///
/// ## Assumptions
/// - Fleet-issue incidents from Customer Support reach Fleet Maintenance (unconfirmed in the context map).
/// - Faults signalled by the bike itself are received by Rentals, which owns locks and stations (unconfirmed).
/// - Rentals owns reserve/unlock; Fleet Maintenance never locks or unlocks bikes itself.
///
/// ## Verification metrics
/// - Time from "reported broken" to "back in circulation".
/// - Share of bikes reported broken again within 7 days of returning to circulation.
///
/// ## Open questions
/// - Can a bike turn out to be unrepairable (retired from the fleet)? Who decides?
/// - Is there a repair priority (e.g. bikes at busy stations first)?
/// - Does Customer Support need an incident id with *Notify Bike Fixed* to resolve the right incident?
///
/// Source canvas: `docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`
@BoundedContext(name = "FleetMaintenance")
@Module
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
