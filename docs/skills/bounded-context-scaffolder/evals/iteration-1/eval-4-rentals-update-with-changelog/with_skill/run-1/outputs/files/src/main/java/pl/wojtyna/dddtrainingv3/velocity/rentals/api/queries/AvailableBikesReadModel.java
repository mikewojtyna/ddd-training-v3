package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

@SecondaryPort
public interface AvailableBikesReadModel {

    Optional<AvailableBikesAtStationView> availableAt(String bikeStationId);
}
