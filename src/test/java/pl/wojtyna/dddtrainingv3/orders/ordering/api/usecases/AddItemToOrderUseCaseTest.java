package pl.wojtyna.dddtrainingv3.orders.ordering.api.usecases;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.Item;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.ItemType;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderEvent.ItemAdded;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderEvent.OrderOpened;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderId;
import pl.wojtyna.dddtrainingv3.orders.ordering.infra.adapters.secondary.InMemoryOrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Add item to order")
class AddItemToOrderUseCaseTest {

    private final List<DomainEvent> published = new ArrayList<>();
    private final DomainEventPublisher publisher = new DomainEventPublisher() {
        @Override
        public void publish(DomainEvents events) {
            events.stream().forEach(published::add);
        }

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }
    };
    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final AddItemToOrderUseCase useCase = new AddItemToOrderUseCase(orders, publisher);

    // @formatter:off
    @DisplayName(
        """
         given no order "order01",
         when a Helmet for 10 PLN is added to it,
         then the order is opened with the Helmet, stored and the events are published
        """
    )
    // @formatter:on
    @Test
    void firstItemOpensTheOrder() {
        // when
        useCase.handle(new AddItemToOrder("order01", "Helmet", BigDecimal.TEN));

        // then
        var id = new OrderId("order01");
        var helmet = new Item(new ItemType("Helmet"), Money.of(CurrencyUnit.of("PLN"), 10));
        assertThat(published).containsExactly(new OrderOpened(id), new ItemAdded(id, helmet));
        assertThat(orders.load(id)).isPresent();
    }
}
