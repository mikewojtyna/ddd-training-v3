package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: one bike, one active holder: a bike someone holds can't be reserved by a technician.
public class BikeAlreadyHeld extends RuntimeException {

    public BikeAlreadyHeld(String message) {
        super(message);
    }

    public BikeAlreadyHeld(String message, Throwable cause) {
        super(message, cause);
    }
}
