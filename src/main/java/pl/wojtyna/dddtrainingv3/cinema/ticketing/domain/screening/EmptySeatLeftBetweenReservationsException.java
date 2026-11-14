package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening;

public class EmptySeatLeftBetweenReservationsException extends RuntimeException {

    public EmptySeatLeftBetweenReservationsException(String message) {
        super(message);
    }

    public EmptySeatLeftBetweenReservationsException(String message, Throwable cause) {
        super(message, cause);
    }
}
