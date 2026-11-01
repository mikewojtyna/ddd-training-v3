package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public record DepositConfirmed(Deposit deposit) implements DomainEvent {}
