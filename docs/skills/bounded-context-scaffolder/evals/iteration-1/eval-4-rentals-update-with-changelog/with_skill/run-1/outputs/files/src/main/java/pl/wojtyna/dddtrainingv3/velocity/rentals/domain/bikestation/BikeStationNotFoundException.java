package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public class BikeStationNotFoundException extends RuntimeException {

    public BikeStationNotFoundException(BikeStationId id) {
        super("Bike station " + id.value() + " not found");
    }
}
