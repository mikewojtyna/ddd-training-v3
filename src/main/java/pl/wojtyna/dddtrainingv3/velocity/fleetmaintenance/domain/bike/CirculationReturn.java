package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;

/// A technician returns a bike to circulation at a place: the moment the bike is fixed (DE-91).
@ValueObject
public record CirculationReturn(TechnicianId technician, Place place, Instant returnedAt) {
}
