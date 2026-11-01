package pl.wojtyna.dddtrainingv3.velocity.deposit.domain.payment;

import pl.wojtyna.dddtrainingv3.velocity.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account.Deposit;

public record PaymentVerified(Deposit deposit) implements DomainEvent {
}
