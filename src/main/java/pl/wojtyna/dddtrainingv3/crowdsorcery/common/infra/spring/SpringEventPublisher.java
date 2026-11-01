package pl.wojtyna.dddtrainingv3.crowdsorcery.common.infra.spring;

import org.springframework.context.ApplicationEventPublisher;
import pl.wojtyna.dddtrainingv3.crowdsorcery.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.crowdsorcery.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.crowdsorcery.common.domain.DomainEventPublisher;

public class SpringEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {this.applicationEventPublisher = applicationEventPublisher;}

    @Override
    public void publish(DomainEvents events) {
        events.stream().forEach(applicationEventPublisher::publishEvent);
    }

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
