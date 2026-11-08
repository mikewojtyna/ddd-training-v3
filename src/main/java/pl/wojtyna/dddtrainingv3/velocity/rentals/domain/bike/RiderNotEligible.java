package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: only a rider eligible to rent (Accounts' answer) reserves or unlocks (DE-80).
public class RiderNotEligible extends RuntimeException {

    public RiderNotEligible(String message) {
        super(message);
    }

    public RiderNotEligible(String message, Throwable cause) {
        super(message, cause);
    }
}
