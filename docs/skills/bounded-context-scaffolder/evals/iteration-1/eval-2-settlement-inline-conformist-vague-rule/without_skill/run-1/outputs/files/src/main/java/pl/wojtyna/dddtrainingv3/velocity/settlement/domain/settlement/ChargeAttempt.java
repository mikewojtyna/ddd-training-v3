package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeResult;

import java.time.Instant;
import java.util.Objects;

/**
 * One try to take money from the rider's payment method. Settlement conforms to Payments, so the outcome is
 * Payments' own {@link ChargeResult}.
 */
@ValueObject
public record ChargeAttempt(int number, Instant attemptedAt, ChargeResult result) {

    public ChargeAttempt {
        if (number < 1) {
            throw new IllegalArgumentException("Charge attempt numbers start at 1");
        }
        Objects.requireNonNull(attemptedAt, "attemptedAt");
        Objects.requireNonNull(result, "result");
    }

    public boolean succeeded() {
        return result instanceof ChargeResult.Succeeded;
    }
}
