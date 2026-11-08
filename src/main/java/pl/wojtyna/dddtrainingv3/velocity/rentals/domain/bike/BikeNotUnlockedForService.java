package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: only a withdrawn bike unlocked for a technician can be made available again.
public class BikeNotUnlockedForService extends RuntimeException {

    public BikeNotUnlockedForService(String message) {
        super(message);
    }

    public BikeNotUnlockedForService(String message, Throwable cause) {
        super(message, cause);
    }
}
