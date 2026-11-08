package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: only the holder of a reservation that hasn't run out unlocks the bike (DE-81).
public class NotReservationHolder extends RuntimeException {

    public NotReservationHolder(String message) {
        super(message);
    }

    public NotReservationHolder(String message, Throwable cause) {
        super(message, cause);
    }
}
