package pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.common.domain.DomainEvent;

public record WithdrawalMade(Money amount) implements DomainEvent {
}
