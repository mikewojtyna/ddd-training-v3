package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

public sealed interface AccountEvent extends DomainEvent {

    @ValueObject
    record AccountBlocked(BorrowerId account) implements AccountEvent {
    }
}
