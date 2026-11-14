package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.Optional;

@Repository
@SecondaryPort
public interface TicketRepository {

    Optional<Ticket> load(TicketNumber number);

    /// Appends {@code newEvents}, the result of the last command (it holds all pending events).
    ///
    /// @throws OptimisticLockingException unless the stored stream is still at {@code ticket.version()}
    void save(Ticket ticket, DomainEvents newEvents);
}
