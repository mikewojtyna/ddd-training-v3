package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Optional;

/// What Payments answered to a charge: it succeeded, or it failed for a reason.
@ValueObject
public record ChargeOutcome(boolean succeeded, Optional<ChargeFailureReason> failureReason) {

    public static ChargeOutcome success() {
        return new ChargeOutcome(true, Optional.empty());
    }

    public static ChargeOutcome failure(ChargeFailureReason reason) {
        return new ChargeOutcome(false, Optional.of(reason));
    }
}
