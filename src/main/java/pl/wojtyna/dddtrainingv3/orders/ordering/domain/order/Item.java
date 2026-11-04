package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

/// One unit added to an order, with an item type and a price.
@ValueObject
public record Item(ItemType type, Money price) {
}
