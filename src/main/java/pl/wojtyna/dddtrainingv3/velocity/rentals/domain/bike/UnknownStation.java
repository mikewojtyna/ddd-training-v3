package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: bikes are added and returned only at a station Rentals knows.
public class UnknownStation extends RuntimeException {

    public UnknownStation(String message) {
        super(message);
    }

    public UnknownStation(String message, Throwable cause) {
        super(message, cause);
    }
}
