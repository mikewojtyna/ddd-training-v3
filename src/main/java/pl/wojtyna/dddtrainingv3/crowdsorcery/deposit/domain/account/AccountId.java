package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

@ValueObject
public record AccountId(UUID value) {
}
