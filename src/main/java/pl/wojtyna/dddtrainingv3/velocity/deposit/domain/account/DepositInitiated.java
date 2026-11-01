package pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.velocity.common.domain.DomainEvent;

@ValueObject
public record DepositInitiated(Deposit deposit) implements DomainEvent {
}
