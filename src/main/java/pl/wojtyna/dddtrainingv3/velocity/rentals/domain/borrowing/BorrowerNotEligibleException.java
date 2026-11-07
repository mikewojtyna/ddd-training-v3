package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing;

/// Rule R6: only a borrower with an activated account can reserve a bike.
public class BorrowerNotEligibleException extends RuntimeException {

    public BorrowerNotEligibleException(String message) {
        super(message);
    }
}
