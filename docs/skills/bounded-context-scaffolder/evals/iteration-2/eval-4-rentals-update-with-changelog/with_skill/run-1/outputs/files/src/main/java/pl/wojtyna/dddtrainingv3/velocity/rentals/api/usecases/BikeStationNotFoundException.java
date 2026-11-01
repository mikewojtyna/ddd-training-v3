package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;

public class BikeStationNotFoundException extends RuntimeException {

    public BikeStationNotFoundException(BikeStationId id) {
        super("Bike station " + id.value() + " not found");
    }
}
