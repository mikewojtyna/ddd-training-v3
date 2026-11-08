package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Bike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeNotFound;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;

import java.util.List;
import java.util.function.Function;

/// The load → command → save → publish steps every Rentals use case on an existing bike shares.
final class BikeCommands {

    private final BikeRepository bikes;
    private final DomainEventPublisher eventPublisher;

    BikeCommands(BikeRepository bikes, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.eventPublisher = eventPublisher;
    }

    DomainEvents execute(String bikeId, Function<Bike, DomainEvents> command) {
        return execute(new BikeId(bikeId), command);
    }

    DomainEvents execute(BikeId bikeId, Function<Bike, DomainEvents> command) {
        var bike = bikes.load(bikeId).orElseThrow(() -> new BikeNotFound(bikeId.value() + " is not part of the fleet"));
        var events = command.apply(bike);
        bikes.save(bike);
        eventPublisher.publish(events);
        return events;
    }

    /// Time-driven work: each bike in its own load-execute-save; a bike changed concurrently (e.g. unlocked or
    /// returned at the same moment) is skipped for this run (plans' D6).
    int executeEach(List<BikeId> bikeIds, Function<Bike, DomainEvents> command) {
        var changed = 0;
        for (var bikeId : bikeIds) {
            try {
                if (execute(bikeId, command).stream().findAny().isPresent()) {
                    changed++;
                }
            } catch (OptimisticLockingException | BikeNotFound skipped) {
                // next run
            }
        }
        return changed;
    }
}
