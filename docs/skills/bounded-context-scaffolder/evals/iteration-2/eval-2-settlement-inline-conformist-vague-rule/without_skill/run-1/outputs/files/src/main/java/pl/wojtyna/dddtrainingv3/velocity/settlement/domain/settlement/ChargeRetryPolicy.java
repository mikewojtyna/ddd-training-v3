package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * "If a charge fails we retry a few times shortly after the ride, then record debt."
 * <p>
 * {@code maxAttempts} counts every charge attempt, including the first one. The {@link #standard()} values are
 * assumptions that still have to be confirmed with the business.
 */
@ValueObject
public record ChargeRetryPolicy(int maxAttempts, Duration delayBetweenAttempts) {

    public ChargeRetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("At least one charge attempt is required");
        }
        Objects.requireNonNull(delayBetweenAttempts, "delayBetweenAttempts");
        if (delayBetweenAttempts.isNegative()) {
            throw new IllegalArgumentException("Delay between charge attempts cannot be negative");
        }
    }

    /**
     * 3 attempts in total (1 charge + 2 retries), 15 minutes apart, so the last retry happens 30 minutes after the
     * ride was priced.
     */
    public static ChargeRetryPolicy standard() {
        return new ChargeRetryPolicy(3, Duration.ofMinutes(15));
    }

    boolean allowsAnotherAttemptAfter(int attemptsMade) {
        return attemptsMade < maxAttempts;
    }

    Instant nextAttemptAfter(Instant failedAttemptAt) {
        return failedAttemptAt.plus(delayBetweenAttempts);
    }
}
