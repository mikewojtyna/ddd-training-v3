package pl.wojtyna.dddtrainingv3.crowdsorcery.payment.api;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.crowdsorcery.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.crowdsorcery.common.integration.IntegrationEvent;

@ValueObject
public record PaymentEvent() implements IntegrationEvent, DomainEvent {
}
