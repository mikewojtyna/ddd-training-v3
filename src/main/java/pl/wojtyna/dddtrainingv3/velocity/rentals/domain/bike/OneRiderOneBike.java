package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: a rider holding a reservation or a running session can't reserve another bike (DE-13).
public class OneRiderOneBike extends RuntimeException {

    public OneRiderOneBike(String message) {
        super(message);
    }

    public OneRiderOneBike(String message, Throwable cause) {
        super(message, cause);
    }
}
