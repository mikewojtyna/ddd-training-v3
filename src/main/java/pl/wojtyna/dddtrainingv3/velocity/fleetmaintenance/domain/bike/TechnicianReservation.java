package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;

/// Rentals holds the broken bike for this technician until `until` (Rentals decides the period, DE-11).
@ValueObject
public record TechnicianReservation(TechnicianId technician, Instant until) {
}
