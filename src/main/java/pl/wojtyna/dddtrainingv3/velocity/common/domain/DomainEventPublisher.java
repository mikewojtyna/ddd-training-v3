package pl.wojtyna.dddtrainingv3.velocity.common.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface DomainEventPublisher {

    void publish(DomainEvents events);

    void publish(DomainEvent event);
}
