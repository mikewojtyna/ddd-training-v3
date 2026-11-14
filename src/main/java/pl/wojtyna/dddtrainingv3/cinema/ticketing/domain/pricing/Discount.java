package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.pricing;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainPolicy;

import java.math.RoundingMode;

/// Policy (strategy): how much of the ticket price a customer pays. Students pay 20% less; marketing adds new
/// discounts often, so each one is a variant here and the price calculator stays the same.
@FunctionalInterface
public interface Discount extends DomainPolicy {

    Money applyTo(Money price);

    static Discount none() {
        return price -> price;
    }

    static Discount student() {
        return percent(20);
    }

    /// A closure: the returned discount captures its percentage.
    static Discount percent(int percent) {
        return price -> price.multipliedBy(100 - percent).dividedBy(100, RoundingMode.HALF_UP);
    }
}
