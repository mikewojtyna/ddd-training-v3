package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

import java.util.Optional;

/// What Settlement answered to a collection: whether it collected, and the rider's outstanding debt afterwards when
/// known (an unanswered request leaves it unknown).
@ValueObject
public record CollectionOutcome(boolean collected, Optional<Money> outstandingDebt) {

    public static CollectionOutcome collected(Money outstandingDebtAfter) {
        return new CollectionOutcome(true, Optional.of(outstandingDebtAfter));
    }

    public static CollectionOutcome failed(Money outstandingDebt) {
        return new CollectionOutcome(false, Optional.of(outstandingDebt));
    }

    public static CollectionOutcome unanswered() {
        return new CollectionOutcome(false, Optional.empty());
    }

    /// The debt is gone: collected, or nothing was owed any more (plan `collections-attempts-debt-collection` D5).
    public boolean debtCleared() {
        return collected || outstandingDebt.map(Money::isZero).orElse(false);
    }
}
