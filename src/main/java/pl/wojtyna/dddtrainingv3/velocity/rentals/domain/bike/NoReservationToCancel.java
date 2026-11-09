package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Rule: only a reservation can be cancelled; a bike that is ridden (or free) has none.
public class NoReservationToCancel extends RuntimeException {

    public NoReservationToCancel(String message) {
        super(message);
    }

    public NoReservationToCancel(String message, Throwable cause) {
        super(message, cause);
    }
}
