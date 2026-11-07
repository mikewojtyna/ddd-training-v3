package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing;

/// Rule R8: a borrower who holds a reservation can rent only the reserved bike.
public class HoldsReservationForAnotherBikeException extends RuntimeException {

    public HoldsReservationForAnotherBikeException(String message) {
        super(message);
    }
}
