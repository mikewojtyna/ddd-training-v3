package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningId;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.SeatNumber;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface TicketEvent extends DomainEvent {

    record TicketIssued(TicketNumber ticketNumber, ScreeningId screeningId, SeatNumber seatNumber) implements TicketEvent {
    }

    record TicketScanned(TicketNumber ticketNumber) implements TicketEvent {
    }
}
