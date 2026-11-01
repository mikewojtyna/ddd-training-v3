package pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account;

import pl.wojtyna.dddtrainingv3.velocity.common.domain.DomainEvent;

public record DepositConfirmed(Deposit deposit) implements DomainEvent {}
