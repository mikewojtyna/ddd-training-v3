package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderEvent.ItemAdded;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderEvent.OrderOpened;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Order examples")
class OrderTest {

    private static final OrderId ORDER_ID = new OrderId("order01");
    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    // @formatter:off
    @DisplayName(
        """
         given an empty order,
         when an item for 100 PLN is added,
         then the item is added
        """
    )
    // @formatter:on
    @Test
    void itemForExactly100PlnFitsIntoEmptyOrder() {
        // given
        var order = Order.open(ORDER_ID);
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 100));

        // when
        var events = order.add(helmet);

        // then
        assertThat(events.stream()).containsExactly(new OrderOpened(ORDER_ID), new ItemAdded(ORDER_ID, helmet));
    }

    // @formatter:off
    @DisplayName(
        """
         given an order with items totalling 90 PLN,
         when an item for 20 PLN is added,
         then adding the item is rejected
        """
    )
    // @formatter:on
    @Test
    void itemPushingTotalAbove100PlnIsRejected() {
        // given
        var order = Order.open(ORDER_ID);
        order.add(new Item(new ItemType("Helmet"), Money.of(PLN, 50)));
        order.add(new Item(new ItemType("Lock"), Money.of(PLN, 40)));
        var light = new Item(new ItemType("Light"), Money.of(PLN, 20));

        // when
        var exception = catchThrowable(() -> order.add(light));

        // then
        assertThat(exception).isInstanceOf(OrderTotalLimitExceededException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given an order with a Helmet for 50 PLN added,
         when a Lock for 40 PLN is added,
         then all events since the order was opened are returned, in order
        """
    )
    // @formatter:on
    @Test
    void everyCommandReturnsAllPendingEvents() {
        // given
        var order = Order.open(ORDER_ID);
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 50));
        order.add(helmet);
        var lock = new Item(new ItemType("Lock"), Money.of(PLN, 40));

        // when
        var events = order.add(lock);

        // then
        assertThat(events.stream()).containsExactly(new OrderOpened(ORDER_ID),
                                                    new ItemAdded(ORDER_ID, helmet),
                                                    new ItemAdded(ORDER_ID, lock));
    }

    // @formatter:off
    @DisplayName(
        """
         given an order with 2 "Helmet" items,
         when another "Helmet" is added,
         then the item is added
        """
    )
    // @formatter:on
    @Test
    void thirdItemOfTheSameTypeIsAdded() {
        // given
        var order = Order.open(ORDER_ID);
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 10));
        order.add(helmet);
        order.add(helmet);

        // when
        var events = order.add(helmet);

        // then
        assertThat(events.stream()).containsExactly(new OrderOpened(ORDER_ID),
                                                    new ItemAdded(ORDER_ID, helmet),
                                                    new ItemAdded(ORDER_ID, helmet),
                                                    new ItemAdded(ORDER_ID, helmet));
    }

    // @formatter:off
    @DisplayName(
        """
         given an order with 3 "Helmet" items,
         when another "Helmet" is added,
         then adding the item is rejected
        """
    )
    // @formatter:on
    @Test
    void fourthItemOfTheSameTypeIsRejected() {
        // given
        var order = Order.open(ORDER_ID);
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 10));
        order.add(helmet);
        order.add(helmet);
        order.add(helmet);

        // when
        var exception = catchThrowable(() -> order.add(helmet));

        // then
        assertThat(exception).isInstanceOf(ItemTypeLimitExceededException.class);
    }
}
