package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

/// Thrown when adding an item would make the order total exceed 100 PLN.
public class OrderTotalLimitExceededException extends RuntimeException {

    public OrderTotalLimitExceededException(String message) {
        super(message);
    }
}
