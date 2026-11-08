package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// The bike isn't part of the fleet.
public class BikeNotFound extends RuntimeException {

    public BikeNotFound(String message) {
        super(message);
    }

    public BikeNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}
