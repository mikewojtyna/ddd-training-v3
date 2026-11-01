package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import java.math.BigDecimal;

public record FeeEstimate(BigDecimal amount, String currency) {
}
