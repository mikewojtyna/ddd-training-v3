package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// Identity of a bike, in Fleet Maintenance's own terms. Deliberately not Rentals' `BikeId`:
/// contexts talk only through each other's `api` (see `FleetMaintenanceArchitectureTest`).
@ValueObject
public record BikeId(String value) {

    public BikeId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Bike id cannot be blank");
        }
    }
}
