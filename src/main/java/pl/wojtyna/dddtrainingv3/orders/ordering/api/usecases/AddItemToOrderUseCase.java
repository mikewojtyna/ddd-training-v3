package pl.wojtyna.dddtrainingv3.orders.ordering.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.Item;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.ItemType;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.Order;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderId;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderRepository;

/// Adds an item to an order. An order that doesn't exist yet is opened by its first item.
@PrimaryPort
public class AddItemToOrderUseCase {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    private final OrderRepository orders;
    private final DomainEventPublisher eventPublisher;

    public AddItemToOrderUseCase(OrderRepository orders, DomainEventPublisher eventPublisher) {
        this.orders = orders;
        this.eventPublisher = eventPublisher;
    }

    public void handle(AddItemToOrder command) {
        var id = new OrderId(command.orderId());
        var order = orders.load(id).orElseGet(() -> Order.open(id));
        var events = order.add(new Item(new ItemType(command.itemType()), Money.of(PLN, command.pricePln())));
        orders.save(order, events);
        eventPublisher.publish(events);
    }
}
