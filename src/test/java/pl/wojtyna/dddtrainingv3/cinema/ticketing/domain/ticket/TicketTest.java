package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningId;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.SeatNumber;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketEvent.TicketIssued;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketEvent.TicketScanned;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Ticket examples")
class TicketTest {

    private static final TicketNumber T1 = new TicketNumber("T-1");
    private static final ScreeningId S1 = new ScreeningId("S1");
    private static final SeatNumber A7 = SeatNumber.of("A7");

    // @formatter:off
    @DisplayName(
        """
         given ticket T-1 issued for S1 / A7,
         when it is scanned at the entrance,
         then the ticket is scanned (all events since it was issued are returned, in order)
        """
    )
    // @formatter:on
    @Test
    void issuedTicketIsScanned() {
        // given
        var ticket = Ticket.issue(T1, S1, A7).created();

        // when
        var events = ticket.scan();

        // then
        assertThat(events.stream()).containsExactly(new TicketIssued(T1, S1, A7), new TicketScanned(T1));
    }

    // @formatter:off
    @DisplayName(
        """
         given ticket T-1 already scanned at the entrance,
         when it is scanned again,
         then it is rejected: the ticket is already used
        """
    )
    // @formatter:on
    @Test
    void ticketIsScannedOnlyOnce() {
        // given
        var ticket = Ticket.rebuild(T1, List.of(new TicketIssued(T1, S1, A7), new TicketScanned(T1)));

        // when
        var exception = catchThrowable(ticket::scan);

        // then
        assertThat(exception).isInstanceOf(TicketAlreadyUsedException.class);
    }
}
