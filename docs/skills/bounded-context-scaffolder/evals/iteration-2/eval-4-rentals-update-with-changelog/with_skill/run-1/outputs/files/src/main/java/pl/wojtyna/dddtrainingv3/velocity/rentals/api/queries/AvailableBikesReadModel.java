package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface AvailableBikesReadModel {

    /// Returns the available bikes at the station; an unknown station has no available bikes.
    AvailableBikesAtStation availableAt(String stationId);
}
