package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface OrderEvent extends DomainEvent {

    record OrderOpened(OrderId orderId) implements OrderEvent {
    }

    record ItemAdded(OrderId orderId, Item item) implements OrderEvent {
    }
}
