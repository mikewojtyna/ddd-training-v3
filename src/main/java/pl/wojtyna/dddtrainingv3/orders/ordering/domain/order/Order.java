package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Event-sourced order. Guards: the order total cannot exceed 100 PLN, and at most 3 items of the same item type.
@AggregateRoot
public class Order {

    private static final Money ORDER_TOTAL_LIMIT = Money.of(CurrencyUnit.of("PLN"), 100);
    private static final int ITEMS_PER_TYPE_LIMIT = 3;

    private final OrderId id;
    private final Version version; // the version this order was opened or loaded at; commands never change it
    private DomainEvents pendingEvents; // every event emitted since open, in order
    private Money total = Money.zero(CurrencyUnit.of("PLN"));
    private final Map<ItemType, Integer> itemsPerType = new HashMap<>();

    private Order(OrderId id, Version version, DomainEvents domainEvents) {
        this.id = id;
        this.version = version;
        this.pendingEvents = domainEvents;
    }

    public static Order open(OrderId id) {
        return new Order(id, Version.initial(), DomainEvents.of(new OrderEvent.OrderOpened(id)));
    }

    /// Restores an order from its stored history. The history is already stored, so it is not pending.
    public static Order rebuild(OrderId id, List<OrderEvent> history) {
        var order = new Order(id, Version.initial().plus(history.size()), DomainEvents.empty());
        history.forEach(order::apply);
        return order;
    }

    public DomainEvents add(Item item) {
        if (total.plus(item.price()).isGreaterThan(ORDER_TOTAL_LIMIT)) {
            throw new OrderTotalLimitExceededException("Order total cannot exceed " + ORDER_TOTAL_LIMIT);
        }
        if (itemsPerType.getOrDefault(item.type(), 0) >= ITEMS_PER_TYPE_LIMIT) {
            throw new ItemTypeLimitExceededException("Order cannot hold more than " + ITEMS_PER_TYPE_LIMIT + " items of type " + item.type().name());
        }
        var itemAdded = new OrderEvent.ItemAdded(id, item);
        apply(itemAdded);
        pendingEvents = pendingEvents.append(DomainEvents.of(itemAdded));
        return pendingEvents;
    }

    private void apply(OrderEvent event) {
        switch (event) {
            case OrderEvent.OrderOpened opened -> {
            }
            case OrderEvent.ItemAdded added -> {
                total = total.plus(added.item().price());
                itemsPerType.merge(added.item().type(), 1, Integer::sum);
            }
        }
    }

    @Identity
    public OrderId id() {
        return id;
    }

    public Version version() {
        return version;
    }
}
