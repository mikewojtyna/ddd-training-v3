package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.pricing;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Ticket price examples")
class TicketPriceCalculatorTest {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 11, 18);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 21);

    // @formatter:off
    @DisplayName(
        """
         given a screening on Wednesday 2026-11-18,
         when the ticket price is calculated,
         then it is 25 PLN
        """
    )
    // @formatter:on
    @Test
    void weekdayTicketCosts25Pln() {
        // given
        var calculator = new TicketPriceCalculator();

        // when
        var price = calculator.priceFor(WEDNESDAY, Discount.none());

        // then
        assertThat(price).isEqualTo(Money.of(PLN, 25));
    }

    // @formatter:off
    @DisplayName(
        """
         given a screening on Saturday 2026-11-21,
         when the ticket price is calculated,
         then it is 30 PLN
        """
    )
    // @formatter:on
    @Test
    void weekendTicketCosts30Pln() {
        // given
        var calculator = new TicketPriceCalculator();

        // when
        var price = calculator.priceFor(SATURDAY, Discount.none());

        // then
        assertThat(price).isEqualTo(Money.of(PLN, 30));
    }

    // @formatter:off
    @DisplayName(
        """
         given a Saturday screening,
         when a customer without a discount buys a ticket,
         then the customer pays 30 PLN
        """
    )
    // @formatter:on
    @Test
    void customerWithoutDiscountPaysFullPrice() {
        // given
        var calculator = new TicketPriceCalculator();

        // when
        var price = calculator.priceFor(SATURDAY, Discount.none());

        // then
        assertThat(price).isEqualTo(Money.of(PLN, 30));
    }

    // @formatter:off
    @DisplayName(
        """
         given a Saturday screening,
         when a student buys a ticket,
         then the student pays 24 PLN
        """
    )
    // @formatter:on
    @Test
    void studentPays20PercentLess() {
        // given
        var calculator = new TicketPriceCalculator();

        // when
        var price = calculator.priceFor(SATURDAY, Discount.student());

        // then
        assertThat(price).isEqualTo(Money.of(PLN, 24));
    }
}
