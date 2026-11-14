package pl.wojtyna.dddtrainingv3.cinema.ticketing.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.Ticket;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketEvent;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketNumber;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketRepository;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Event store in memory: one event stream per ticket.
@SecondaryAdapter
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<TicketNumber, List<TicketEvent>> streams = new ConcurrentHashMap<>();

    @Override
    public Optional<Ticket> load(TicketNumber number) {
        return Optional.ofNullable(streams.get(number)).map(history -> Ticket.rebuild(number, List.copyOf(history)));
    }

    @Override
    public synchronized void save(Ticket ticket, DomainEvents newEvents) {
        var stream = streams.computeIfAbsent(ticket.number(), number -> new ArrayList<>());
        if (stream.size() != ticket.version().value()) {
            throw new OptimisticLockingException("Ticket " + ticket.number().value() + " was modified concurrently");
        }
        newEvents.stream().map(TicketEvent.class::cast).forEach(stream::add);
    }
}
