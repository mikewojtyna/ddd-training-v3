package pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

@ValueObject
public record AccountId(UUID value) {
}
