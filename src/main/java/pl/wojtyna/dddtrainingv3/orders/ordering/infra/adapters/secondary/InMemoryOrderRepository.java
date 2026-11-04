package pl.wojtyna.dddtrainingv3.orders.ordering.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.Order;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderEvent;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderId;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Event store in memory: one event stream per order.
@SecondaryAdapter
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, List<OrderEvent>> streams = new ConcurrentHashMap<>();

    @Override
    public Optional<Order> load(OrderId id) {
        return Optional.ofNullable(streams.get(id)).map(history -> Order.rebuild(id, List.copyOf(history)));
    }

    @Override
    public synchronized void save(Order order, DomainEvents newEvents) {
        var stream = streams.computeIfAbsent(order.id(), id -> new ArrayList<>());
        if (stream.size() != order.version().value()) {
            throw new OptimisticLockingException("Order " + order.id().value() + " was modified concurrently");
        }
        newEvents.stream().map(OrderEvent.class::cast).forEach(stream::add);
    }
}
