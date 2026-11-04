package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

/// Thrown when adding an item would put more than 3 items of the same item type into the order.
public class ItemTypeLimitExceededException extends RuntimeException {

    public ItemTypeLimitExceededException(String message) {
        super(message);
    }
}
