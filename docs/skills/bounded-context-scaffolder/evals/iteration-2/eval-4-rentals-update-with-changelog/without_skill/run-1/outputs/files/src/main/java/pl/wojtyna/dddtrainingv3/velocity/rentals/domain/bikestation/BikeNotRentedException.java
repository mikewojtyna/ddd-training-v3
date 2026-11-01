package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public class BikeNotRentedException extends RuntimeException {

    public BikeNotRentedException(String message) {
        super(message);
    }

    public BikeNotRentedException(String message, Throwable cause) {
        super(message, cause);
    }
}
