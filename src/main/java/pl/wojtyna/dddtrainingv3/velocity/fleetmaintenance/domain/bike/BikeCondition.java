package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

/// Where a bike stands in Fleet Maintenance.
@ValueObject
public enum BikeCondition {
    /// Reported faulty; riders can't rent it; waits for a technician.
    BROKEN,
    /// A technician's unlock started maintenance.
    UNDER_MAINTENANCE,
    /// Not broken: never reported, or fixed and back in circulation.
    IN_CIRCULATION
}
