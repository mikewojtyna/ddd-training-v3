package pl.wojtyna.dddtrainingv3.cinema.ticketing.infra.adapters.secondary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningId;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.SeatNumber;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.Ticket;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketAlreadyUsedException;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketNumber;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("In-memory ticket repository examples")
class InMemoryTicketRepositoryTest {

    private static final TicketNumber T42 = new TicketNumber("T-42");
    private static final ScreeningId S1 = new ScreeningId("S1");
    private static final SeatNumber A7 = SeatNumber.of("A7");

    // @formatter:off
    @DisplayName(
        """
         given ticket T-42 saved after its scan,
         when it is loaded and scanned again,
         then it is rejected: the loaded ticket remembers its scan
        """
    )
    // @formatter:on
    @Test
    void savedTicketLoadsBackWithItsHistory() {
        // given
        var repository = new InMemoryTicketRepository();
        var ticket = Ticket.issue(T42, S1, A7).created();
        repository.save(ticket, ticket.scan());

        // when
        var exception = catchThrowable(() -> repository.load(T42).orElseThrow().scan());

        // then
        assertThat(exception).isInstanceOf(TicketAlreadyUsedException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given two copies of ticket T-42 loaded and both scanned,
         when the first copy is saved and then the second,
         then the second save fails: the ticket changed after it was loaded
        """
    )
    // @formatter:on
    @Test
    void staleWriteIsRefused() {
        // given
        var repository = new InMemoryTicketRepository();
        var issued = Ticket.issue(T42, S1, A7);
        repository.save(issued.created(), issued.events());
        var first = repository.load(T42).orElseThrow();
        var second = repository.load(T42).orElseThrow();
        var firstScan = first.scan();
        var secondScan = second.scan();
        repository.save(first, firstScan);

        // when
        var exception = catchThrowable(() -> repository.save(second, secondScan));

        // then
        assertThat(exception).isInstanceOf(OptimisticLockingException.class);
    }
}
