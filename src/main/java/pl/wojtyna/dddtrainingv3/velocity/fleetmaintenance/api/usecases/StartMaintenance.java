package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import java.time.Instant;

/// Rentals reported a technician's unlock (Bike Unlocked, technicians only): maintenance starts.
public record StartMaintenance(String bikeId, String technicianId, Instant startedAt) {
}
