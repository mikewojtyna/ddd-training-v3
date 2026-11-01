package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.payment;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account.Deposit;

public record PaymentVerified(Deposit deposit) implements DomainEvent {
}
