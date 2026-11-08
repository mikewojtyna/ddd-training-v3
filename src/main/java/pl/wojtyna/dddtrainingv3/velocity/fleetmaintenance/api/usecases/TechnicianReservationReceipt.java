package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import java.time.Instant;

/// The answer to *Reserve Broken Bike*: until when Rentals holds the bike for the technician.
public record TechnicianReservationReceipt(String bikeId, String technicianId, Instant reservedUntil) {
}
