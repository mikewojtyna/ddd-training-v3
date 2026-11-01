package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public enum AccountStatus {
    ACTIVE,
    BLOCKED
}
