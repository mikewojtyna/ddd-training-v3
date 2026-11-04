package pl.wojtyna.dddtrainingv3.orders.ordering.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.orders.ordering.api.usecases.AddItemToOrderUseCase;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderRepository;
import pl.wojtyna.dddtrainingv3.orders.ordering.infra.adapters.secondary.InMemoryOrderRepository;

@Configuration
public class OrderingContextConfig {

    @Bean
    public OrderRepository orderingOrderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    public AddItemToOrderUseCase orderingAddItemToOrderUseCase(OrderRepository orderingOrderRepository,
                                                               DomainEventPublisher eventPublisher) {
        return new AddItemToOrderUseCase(orderingOrderRepository, eventPublisher);
    }
}
