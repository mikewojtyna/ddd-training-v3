package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

@ValueObject
public record Deposit(Money amount, AccountId accountId) {
}
