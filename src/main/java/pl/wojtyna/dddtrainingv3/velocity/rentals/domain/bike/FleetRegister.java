package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Which bikes already joined the fleet. Implemented by the bike repository.
@SecondaryPort
public interface FleetRegister {

    boolean contains(BikeId bike);
}
