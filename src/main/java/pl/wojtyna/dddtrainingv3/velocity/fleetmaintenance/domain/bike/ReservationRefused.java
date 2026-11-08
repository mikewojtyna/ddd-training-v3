package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Rentals refused a technician's reservation for a reason Fleet Maintenance has no word for.
public class ReservationRefused extends RuntimeException {

    public ReservationRefused(String message) {
        super(message);
    }

    public ReservationRefused(String message, Throwable cause) {
        super(message, cause);
    }
}
