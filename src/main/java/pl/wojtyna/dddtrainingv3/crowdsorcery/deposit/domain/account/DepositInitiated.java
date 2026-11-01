package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

@ValueObject
public record DepositInitiated(Deposit deposit) implements DomainEvent {
}
