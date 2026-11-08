package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: a bike joins the fleet only once.
public class BikeAlreadyInFleet extends RuntimeException {

    public BikeAlreadyInFleet(String message) {
        super(message);
    }

    public BikeAlreadyInFleet(String message, Throwable cause) {
        super(message, cause);
    }
}
