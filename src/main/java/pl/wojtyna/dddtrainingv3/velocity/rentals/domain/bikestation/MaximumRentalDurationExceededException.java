package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public class MaximumRentalDurationExceededException extends RuntimeException {

    public MaximumRentalDurationExceededException(String message) {
        super(message);
    }

    public MaximumRentalDurationExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
