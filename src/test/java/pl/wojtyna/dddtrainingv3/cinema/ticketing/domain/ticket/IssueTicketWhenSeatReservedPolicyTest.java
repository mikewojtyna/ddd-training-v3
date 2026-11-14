package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.CustomerId;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningEvent.SeatReserved;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningId;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.SeatNumber;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketEvent.TicketIssued;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.infra.adapters.secondary.InMemoryTicketRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Issue a ticket when a seat is reserved")
class IssueTicketWhenSeatReservedPolicyTest {

    private static final ScreeningId S1 = new ScreeningId("S1");
    private static final SeatNumber A7 = SeatNumber.of("A7");
    private static final TicketNumber T42 = new TicketNumber("T-42");

    // @formatter:off
    @DisplayName(
        """
         given the ticket sequence is at 41,
         when SeatReserved (S1, A7, Ann) is handled,
         then ticket T-42 for S1 / A7 is stored and TicketIssued is published
        """
    )
    // @formatter:on
    @Test
    void reservedSeatGetsATicket() {
        // given
        var repository = new InMemoryTicketRepository();
        var publisher = new RecordingEventPublisher();
        var policy = new IssueTicketWhenSeatReservedPolicy(new TicketFactory(new TicketSequenceAt(41)), repository, publisher);

        // when
        policy.handle(new SeatReserved(S1, A7, new CustomerId("Ann")));

        // then
        assertThat(repository.load(T42)).isPresent();
        assertThat(publisher.published).containsExactly(new TicketIssued(T42, S1, A7));
    }
}
