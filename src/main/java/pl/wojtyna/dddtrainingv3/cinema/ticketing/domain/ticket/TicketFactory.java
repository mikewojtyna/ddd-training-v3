package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import org.jmolecules.ddd.annotation.Factory;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningId;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.SeatNumber;

/// Factory: a new ticket is numbered T-<n>, where n is the next number of the cinema's ticket sequence.
@Factory
public class TicketFactory {

    private final TicketSequence sequence;

    public TicketFactory(TicketSequence sequence) {
        this.sequence = sequence;
    }

    public Ticket.NewTicket issueFor(ScreeningId screening, SeatNumber seat) {
        return Ticket.issue(new TicketNumber("T-" + sequence.next()), screening, seat);
    }
}
