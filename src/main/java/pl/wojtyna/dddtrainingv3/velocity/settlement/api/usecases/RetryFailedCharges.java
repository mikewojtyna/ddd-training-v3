package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import java.time.Instant;

/// *Retry Failed Charges*: charge again every ride whose next attempt is due at `now` (DE-35).
public record RetryFailedCharges(Instant now) {
}
