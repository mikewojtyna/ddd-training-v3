package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

public class TicketAlreadyUsedException extends RuntimeException {

    public TicketAlreadyUsedException(String message) {
        super(message);
    }

    public TicketAlreadyUsedException(String message, Throwable cause) {
        super(message, cause);
    }
}
