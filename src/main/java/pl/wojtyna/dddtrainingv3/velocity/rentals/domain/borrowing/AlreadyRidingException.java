package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing;

/// Rule R7: a borrower who is riding a bike can't reserve another one.
public class AlreadyRidingException extends RuntimeException {

    public AlreadyRidingException(String message) {
        super(message);
    }
}
