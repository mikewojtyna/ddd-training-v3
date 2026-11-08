package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import java.time.Instant;

/// Rentals reported that a technician's reservation ended without an unlock (Reservation Expired, Unlock Failed).
public record ReleaseTechnicianReservation(String bikeId, String technicianId, Instant releasedAt) {
}
