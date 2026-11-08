package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: only the rider whose session runs on the bike can end it.
public class NoRunningSession extends RuntimeException {

    public NoRunningSession(String message) {
        super(message);
    }

    public NoRunningSession(String message, Throwable cause) {
        super(message, cause);
    }
}
