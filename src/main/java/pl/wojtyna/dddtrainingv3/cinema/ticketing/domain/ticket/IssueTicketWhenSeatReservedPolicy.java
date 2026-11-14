package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningEvent.SeatReserved;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainPolicy;

/// Policy (reacting): whenever a seat is reserved, a ticket is issued for it. Screening and Ticket are separate
/// aggregates, so the ticket follows the reservation eventually, in its own transaction.
public class IssueTicketWhenSeatReservedPolicy implements DomainPolicy {

    private final TicketFactory tickets;
    private final TicketRepository repository;
    private final DomainEventPublisher eventPublisher;

    public IssueTicketWhenSeatReservedPolicy(TicketFactory tickets, TicketRepository repository, DomainEventPublisher eventPublisher) {
        this.tickets = tickets;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void handle(SeatReserved seatReserved) {
        var newTicket = tickets.issueFor(seatReserved.screeningId(), seatReserved.seatNumber());
        repository.save(newTicket.created(), newTicket.events());
        eventPublisher.publish(newTicket.events());
    }
}
