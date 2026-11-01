package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import java.math.BigDecimal;

/// Result of the "Estimate Fee" query. Return charges are not included, because they're only known after the ride.
public record FeeEstimate(String priceListId, BigDecimal amount, String currency) {
}
