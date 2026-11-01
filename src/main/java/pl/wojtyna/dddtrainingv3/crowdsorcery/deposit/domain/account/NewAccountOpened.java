package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account;

import pl.wojtyna.dddtrainingv3.crowdsorcery.common.domain.DomainEvent;

public record NewAccountOpened(DepositAccount account) implements DomainEvent {
}
