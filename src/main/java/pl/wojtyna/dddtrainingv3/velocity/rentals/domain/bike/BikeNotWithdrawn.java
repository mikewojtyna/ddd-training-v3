package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: a technician reserves only withdrawn bikes (DE-16).
public class BikeNotWithdrawn extends RuntimeException {

    public BikeNotWithdrawn(String message) {
        super(message);
    }

    public BikeNotWithdrawn(String message, Throwable cause) {
        super(message, cause);
    }
}
