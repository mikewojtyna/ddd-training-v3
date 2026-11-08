package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

import java.math.BigDecimal;

/// Inbound command *Charge* (from Settlement): charge `amount` PLN to the rider's current card, for `reference`
/// (Settlement's ride).
public record ChargeCurrentCard(String riderId, BigDecimal amount, String reference) {
}
