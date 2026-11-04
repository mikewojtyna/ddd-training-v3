package pl.wojtyna.dddtrainingv3.orders.ordering.infra.adapters.primary;

import java.math.BigDecimal;

public record AddItemRequest(String itemType, BigDecimal pricePln) {
}
