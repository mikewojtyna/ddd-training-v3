package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.ValueObject;

/// The kind of product an item is, e.g. "Helmet". Items of the same type count towards the same limit.
@ValueObject
public record ItemType(String name) {
}
