package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.ScreeningEvent.SeatReserved;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Screening examples")
class ScreeningTest {

    private static final ScreeningId S1 = new ScreeningId("S1");
    private static final CustomerId ANN = new CustomerId("Ann");
    private static final CustomerId BOB = new CustomerId("Bob");

    // @formatter:off
    @DisplayName(
        """
         given screening S1 with A5 reserved,
         when Ann reserves A6,
         then the seat is reserved
        """
    )
    // @formatter:on
    @Test
    void seatNextToAReservedOneCanBeReserved() {
        // given
        var screening = Screening.schedule(S1);
        screening.reserve(SeatNumber.of("A5")).by(BOB);

        // when
        var events = screening.reserve(SeatNumber.of("A6")).by(ANN);

        // then
        assertThat(events.stream()).containsExactly(new SeatReserved(S1, SeatNumber.of("A6"), ANN));
    }

    // @formatter:off
    @DisplayName(
        """
         given screening S1 with A5 reserved,
         when Bob reserves A5,
         then the reservation is rejected: the seat is already reserved
        """
    )
    // @formatter:on
    @Test
    void seatIsReservedOnlyOnce() {
        // given
        var screening = Screening.schedule(S1);
        screening.reserve(SeatNumber.of("A5")).by(ANN);

        // when
        var exception = catchThrowable(() -> screening.reserve(SeatNumber.of("A5")).by(BOB));

        // then
        assertThat(exception).isInstanceOf(SeatAlreadyReservedException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given screening S1 with A5 reserved,
         when Ann reserves A7,
         then the reservation is rejected: A6 would stay empty between A5 and A7
        """
    )
    // @formatter:on
    @Test
    void reservationCannotLeaveAnEmptySeatBetweenReservedSeats() {
        // given
        var screening = Screening.schedule(S1);
        screening.reserve(SeatNumber.of("A5")).by(BOB);

        // when
        var exception = catchThrowable(() -> screening.reserve(SeatNumber.of("A7")).by(ANN));

        // then
        assertThat(exception).isInstanceOf(EmptySeatLeftBetweenReservationsException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given screening S1 with A5 reserved,
         when Ann reserves B7,
         then the seat is reserved: B is another row
        """
    )
    // @formatter:on
    @Test
    void gapRuleAppliesWithinARowOnly() {
        // given
        var screening = Screening.schedule(S1);
        screening.reserve(SeatNumber.of("A5")).by(BOB);

        // when
        var events = screening.reserve(SeatNumber.of("B7")).by(ANN);

        // then
        assertThat(events.stream()).containsExactly(new SeatReserved(S1, SeatNumber.of("B7"), ANN));
    }

    // @formatter:off
    @DisplayName(
        """
         given screening S1 with A5 reserved,
         when Ann reserves A9,
         then the reservation is rejected: A6 to A8 would stay empty between A5 and A9
        """
    )
    // @formatter:on
    @Test
    void reservationCannotLeaveSeveralEmptySeatsBetweenReservedSeats() {
        // given
        var screening = Screening.schedule(S1);
        screening.reserve(SeatNumber.of("A5")).by(BOB);

        // when
        var exception = catchThrowable(() -> screening.reserve(SeatNumber.of("A9")).by(ANN));

        // then
        assertThat(exception).isInstanceOf(EmptySeatLeftBetweenReservationsException.class);
    }
}
