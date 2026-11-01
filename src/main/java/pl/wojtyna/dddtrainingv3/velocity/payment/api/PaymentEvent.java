package pl.wojtyna.dddtrainingv3.velocity.payment.api;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.velocity.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.velocity.common.integration.IntegrationEvent;

@ValueObject
public record PaymentEvent() implements IntegrationEvent, DomainEvent {
}
