package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public class BikeAlreadyInStation extends RuntimeException {

    public BikeAlreadyInStation(String message) {
        super(message);
    }

    public BikeAlreadyInStation(String message, Throwable cause) {
        super(message, cause);
    }
}
