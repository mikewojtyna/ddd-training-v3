package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningId;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.SeatNumber;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.util.ArrayList;
import java.util.List;

/// Event-sourced ticket. Guards: a ticket is scanned at the entrance only once.
@AggregateRoot
public class Ticket {

    private final TicketNumber number;
    private final List<TicketEvent> pendingEvents = new ArrayList<>(); // emitted since issue/rebuild, in order
    private boolean scanned; // needed by: scanned only once

    private Ticket(TicketNumber number) {
        this.number = number;
    }

    /// Issuing is an event too: without it, a new ticket has no stream to store or load.
    public static NewTicket issue(TicketNumber number, ScreeningId screening, SeatNumber seat) {
        var ticket = new Ticket(number);
        return new NewTicket(ticket, ticket.emit(new TicketEvent.TicketIssued(number, screening, seat)));
    }

    /// Restores a ticket from its stored history. The history is already stored, so it is not pending.
    public static Ticket rebuild(TicketNumber number, List<TicketEvent> history) {
        var ticket = new Ticket(number);
        history.forEach(ticket::apply);
        return ticket;
    }

    public DomainEvents scan() {
        if (scanned) {
            throw new TicketAlreadyUsedException("Ticket " + number.value() + " was already scanned");
        }
        return emit(new TicketEvent.TicketScanned(number));
    }

    /// Applies the event, records it as pending and returns all pending events, not only this one.
    private DomainEvents emit(TicketEvent event) {
        apply(event);
        pendingEvents.add(event);
        return DomainEvents.of(pendingEvents.toArray(DomainEvent[]::new));
    }

    private void apply(TicketEvent event) {
        switch (event) {
            case TicketEvent.TicketIssued issued -> {
            }
            case TicketEvent.TicketScanned scannedEvent -> scanned = true;
        }
    }

    @Identity
    public TicketNumber number() {
        return number;
    }

    public record NewTicket(Ticket created, DomainEvents events) {
    }
}
