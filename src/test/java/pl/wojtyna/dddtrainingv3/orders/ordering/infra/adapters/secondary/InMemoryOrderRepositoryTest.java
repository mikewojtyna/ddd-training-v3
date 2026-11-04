package pl.wojtyna.dddtrainingv3.orders.ordering.infra.adapters.secondary;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("In-memory order repository")
class InMemoryOrderRepositoryTest {

    private static final OrderId ORDER_ID = new OrderId("order01");
    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();

    // @formatter:off
    @DisplayName(
        """
         given an order saved with 3 Helmets,
         when it is loaded,
         then it is at version 4 and still rejects a 4th Helmet
        """
    )
    // @formatter:on
    @Test
    void loadedOrderKeepsItsState() {
        // given
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 10));
        var order = Order.open(ORDER_ID);
        order.add(helmet);
        order.add(helmet);
        repository.save(order, order.add(helmet));

        // when
        var loaded = repository.load(ORDER_ID).orElseThrow();

        // then
        assertThat(loaded.version()).isEqualTo(new Version(4));
        assertThat(catchThrowable(() -> loaded.add(helmet))).isInstanceOf(ItemTypeLimitExceededException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given the same saved order loaded twice and an item added to each copy,
         when both copies are saved,
         then the second save is rejected
        """
    )
    // @formatter:on
    @Test
    void secondConcurrentSaveIsRejected() {
        // given
        var order = Order.open(ORDER_ID);
        repository.save(order, order.add(new Item(new ItemType("Helmet"), Money.of(PLN, 10))));
        var first = repository.load(ORDER_ID).orElseThrow();
        var second = repository.load(ORDER_ID).orElseThrow();
        var firstEvents = first.add(new Item(new ItemType("Lock"), Money.of(PLN, 20)));
        var secondEvents = second.add(new Item(new ItemType("Light"), Money.of(PLN, 15)));
        repository.save(first, firstEvents);

        // when
        var exception = catchThrowable(() -> repository.save(second, secondEvents));

        // then
        assertThat(exception).isInstanceOf(OptimisticLockingException.class);
    }
}
