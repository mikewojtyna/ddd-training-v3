package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.util.ArrayList;
import java.util.List;

/// Records published domain events instead of publishing them.
class RecordingEventPublisher implements DomainEventPublisher {

    final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(DomainEvents events) {
        events.stream().forEach(published::add);
    }

    @Override
    public void publish(DomainEvent event) {
        published.add(event);
    }
}
