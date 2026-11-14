package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningId;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.SeatNumber;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket.TicketEvent.TicketIssued;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Ticket factory examples")
class TicketFactoryTest {

    private static final ScreeningId S1 = new ScreeningId("S1");
    private static final SeatNumber A7 = SeatNumber.of("A7");

    // @formatter:off
    @DisplayName(
        """
         given the ticket sequence is at 41,
         when a ticket is issued for S1 / A7,
         then ticket T-42 is issued for S1 / A7
        """
    )
    // @formatter:on
    @Test
    void newTicketGetsTheNextNumberFromTheSequence() {
        // given
        var factory = new TicketFactory(new TicketSequenceAt(41));

        // when
        var newTicket = factory.issueFor(S1, A7);

        // then
        assertThat(newTicket.events().stream()).containsExactly(new TicketIssued(new TicketNumber("T-42"), S1, A7));
    }
}
