package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening;

public class SeatAlreadyReservedException extends RuntimeException {

    public SeatAlreadyReservedException(String message) {
        super(message);
    }

    public SeatAlreadyReservedException(String message, Throwable cause) {
        super(message, cause);
    }
}
