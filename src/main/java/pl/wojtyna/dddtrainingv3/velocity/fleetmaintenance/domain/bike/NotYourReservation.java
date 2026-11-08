package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Rentals' answer: only the technician holding an unexpired reservation of the bike can unlock it (DE-81).
public class NotYourReservation extends RuntimeException {

    public NotYourReservation(String message) {
        super(message);
    }

    public NotYourReservation(String message, Throwable cause) {
        super(message, cause);
    }
}
