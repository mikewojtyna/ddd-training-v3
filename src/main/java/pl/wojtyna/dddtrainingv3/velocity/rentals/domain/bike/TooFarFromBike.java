package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: a rider reserves only a bike within 50 metres (DE-12).
public class TooFarFromBike extends RuntimeException {

    public TooFarFromBike(String message) {
        super(message);
    }

    public TooFarFromBike(String message, Throwable cause) {
        super(message, cause);
    }
}
