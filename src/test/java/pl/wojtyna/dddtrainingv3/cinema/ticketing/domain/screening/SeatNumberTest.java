package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Seat number examples")
class SeatNumberTest {

    // @formatter:off
    @DisplayName(
        """
         given "A7",
         when a seat number is made from it,
         then it is row A, seat 7
        """
    )
    // @formatter:on
    @Test
    void seatNumberIsARowAndASeat() {
        // given
        var text = "A7";

        // when
        var seatNumber = SeatNumber.of(text);

        // then
        assertThat(seatNumber.row()).isEqualTo('A');
        assertThat(seatNumber.seat()).isEqualTo(7);
    }

    // @formatter:off
    @DisplayName(
        """
         given "K3",
         when a seat number is made from it,
         then it is rejected: there is no row K
        """
    )
    // @formatter:on
    @Test
    void rowOutsideTheHallIsRejected() {
        // given
        var text = "K3";

        // when
        var exception = catchThrowable(() -> SeatNumber.of(text));

        // then
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given two seat numbers made from "A7",
         when they are compared,
         then they are equal
        """
    )
    // @formatter:on
    @Test
    void seatNumbersAreEqualByValue() {
        // given
        var first = SeatNumber.of("A7");
        var second = SeatNumber.of("A7");

        // when
        var equal = first.equals(second);

        // then
        assertThat(equal).isTrue();
        assertThat(first).hasSameHashCodeAs(second);
    }
}
