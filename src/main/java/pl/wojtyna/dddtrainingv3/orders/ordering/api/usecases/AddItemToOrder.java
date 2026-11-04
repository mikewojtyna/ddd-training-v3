package pl.wojtyna.dddtrainingv3.orders.ordering.api.usecases;

import java.math.BigDecimal;

/// Canvas command *Add item to order*. The price is in PLN.
public record AddItemToOrder(String orderId, String itemType, BigDecimal pricePln) {
}
