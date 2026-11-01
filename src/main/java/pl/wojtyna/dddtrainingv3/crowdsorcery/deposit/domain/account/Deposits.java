package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account;

import pl.wojtyna.dddtrainingv3.crowdsorcery.common.domain.DomainEvents;

public interface Deposits {

    DomainEvents openNewAccount();
}
