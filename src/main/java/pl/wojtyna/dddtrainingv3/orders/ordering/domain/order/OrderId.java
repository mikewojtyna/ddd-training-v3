package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record OrderId(String value) {
}
