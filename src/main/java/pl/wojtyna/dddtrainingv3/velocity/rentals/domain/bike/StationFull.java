package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: a station docks at most its capacity of bikes; a full station turns a returned bike away.
public class StationFull extends RuntimeException {

    public StationFull(String message) {
        super(message);
    }

    public StationFull(String message, Throwable cause) {
        super(message, cause);
    }
}
