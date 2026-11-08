package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: a rider reserves only an available bike nobody holds (reserved, ridden or withdrawn bikes are rejected).
public class BikeNotAvailable extends RuntimeException {

    public BikeNotAvailable(String message) {
        super(message);
    }

    public BikeNotAvailable(String message, Throwable cause) {
        super(message, cause);
    }
}
