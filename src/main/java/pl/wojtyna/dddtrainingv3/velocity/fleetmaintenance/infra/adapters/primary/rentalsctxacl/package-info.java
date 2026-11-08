/// Anti-corruption layer for Rentals' published language (`rentals.api.events`): Bike Withdrawn becomes a fault
/// report (every reason except `REQUESTED`, which Fleet Maintenance asked for itself); Bike Unlocked, Reservation
/// Expired and Unlock Failed matter only for `TECHNICIAN` holders and become Start Maintenance / Release Technician
/// Reservation. Riders' events are dropped here, before any use case (plan technician-reserves-broken-bike D7).
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
