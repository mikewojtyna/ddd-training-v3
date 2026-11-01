package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.crowdsorcery.common.domain.DomainEvent;

public record WithdrawalMade(Money amount) implements DomainEvent {
}
