package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public class BikeNotAvailableException extends RuntimeException {

    public BikeNotAvailableException(String message) {
        super(message);
    }

    public BikeNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
