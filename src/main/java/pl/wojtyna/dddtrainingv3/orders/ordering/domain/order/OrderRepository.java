package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.Optional;

@Repository
@SecondaryPort
public interface OrderRepository {

    Optional<Order> load(OrderId id);

    /// Appends {@code newEvents}, the result of the last command (it holds all pending events).
    ///
    /// @throws OptimisticLockingException unless the stored stream is still at {@code order.version()}
    void save(Order order, DomainEvents newEvents);
}
