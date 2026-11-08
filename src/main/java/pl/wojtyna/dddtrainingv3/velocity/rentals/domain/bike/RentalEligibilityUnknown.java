package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Accounts couldn't be asked whether the rider is eligible; not knowing means not allowing (plan `rider-reserves-bike` D7).
public class RentalEligibilityUnknown extends RuntimeException {

    public RentalEligibilityUnknown(String message, Throwable cause) {
        super(message, cause);
    }
}
