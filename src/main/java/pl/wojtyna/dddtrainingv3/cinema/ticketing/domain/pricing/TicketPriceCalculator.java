package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.pricing;

import org.jmolecules.ddd.annotation.Service;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/// Domain service: a ticket costs 25 PLN on weekdays and 30 PLN at weekends, less the customer's discount. Which
/// discount applies is the caller's choice: the calculator only applies it.
@Service
public class TicketPriceCalculator {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");
    private static final Money WEEKDAY_PRICE = Money.of(PLN, 25);
    private static final Money WEEKEND_PRICE = Money.of(PLN, 30);
    private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public Money priceFor(LocalDate screeningDay, Discount discount) {
        var price = WEEKEND.contains(screeningDay.getDayOfWeek()) ? WEEKEND_PRICE : WEEKDAY_PRICE;
        return discount.applyTo(price);
    }
}
