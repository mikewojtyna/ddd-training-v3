package pl.wojtyna.dddtrainingv3.velocity.deposit.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.velocity.common.integration.IntegrationEvent;

import java.math.BigDecimal;

@ValueObject
public record DepositMade(String accountId, BigDecimal amount, String currency) implements IntegrationEvent {
}
