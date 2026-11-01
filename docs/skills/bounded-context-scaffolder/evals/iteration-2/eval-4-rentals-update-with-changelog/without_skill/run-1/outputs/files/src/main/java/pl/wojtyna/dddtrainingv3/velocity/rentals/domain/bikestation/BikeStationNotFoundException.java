package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public class BikeStationNotFoundException extends RuntimeException {

    public BikeStationNotFoundException(String message) {
        super(message);
    }

    public BikeStationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
