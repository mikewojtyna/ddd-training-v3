package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import java.math.BigDecimal;

/// Temporary stand-in for Payments' published "Charge Rider" command (plan D1, Q3). Replace with Payments' `api` type.
public record ChargeRider(String rideId, String riderId, BigDecimal amount, String currency) {
}
