package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

import java.time.Instant;

/// A due collection attempt: remind the rider and ask Settlement to collect the whole outstanding debt
/// (`amountAsked`). The use case makes it between `RecoveryProcess#attemptDueAt` and `RecoveryProcess#record`.
@ValueObject
public record CollectionAttempt(RiderId rider, int attemptNumber, Money amountAsked, Instant attemptedAt) {
}
