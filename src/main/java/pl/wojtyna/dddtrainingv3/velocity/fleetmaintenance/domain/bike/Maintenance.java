package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;

/// The work after a technician's unlock; never billed.
@ValueObject
public record Maintenance(TechnicianId technician, Instant startedAt) {
}
