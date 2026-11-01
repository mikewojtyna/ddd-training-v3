package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/// **Charge Attempt**: one try to take money from the rider's payment method.
/// An entity inside `Settlement`, identified by its number within the Settlement. It never changes once made, so it's a record.
@Entity
public record ChargeAttempt(@Identity int number, Outcome outcome) {

    public ChargeAttempt {
        if (number < 1) {
            throw new IllegalArgumentException("Charge Attempt number starts at 1");
        }
        if (outcome == null) {
            throw new IllegalArgumentException("Charge Attempt needs an outcome");
        }
    }

    public boolean failed() {
        return outcome == Outcome.FAILED;
    }

    public enum Outcome {
        SUCCEEDED,
        FAILED
    }
}
