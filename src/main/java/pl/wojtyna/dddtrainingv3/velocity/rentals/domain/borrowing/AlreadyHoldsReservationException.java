package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing;

/// Rule R2: a borrower holds at most one active reservation.
public class AlreadyHoldsReservationException extends RuntimeException {

    public AlreadyHoldsReservationException(String message) {
        super(message);
    }
}
