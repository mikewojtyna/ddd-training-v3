package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Rentals refused to make the bike available again, so the bike isn't fixed.
public class CirculationReturnRefused extends RuntimeException {

    public CirculationReturnRefused(String message) {
        super(message);
    }

    public CirculationReturnRefused(String message, Throwable cause) {
        super(message, cause);
    }
}
