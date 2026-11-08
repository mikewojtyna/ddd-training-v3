package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Rentals' answer: someone else holds the bike (one bike, one holder).
public class AlreadyReserved extends RuntimeException {

    public AlreadyReserved(String message) {
        super(message);
    }

    public AlreadyReserved(String message, Throwable cause) {
        super(message, cause);
    }
}
