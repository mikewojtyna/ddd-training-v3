package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Position;

import java.math.BigDecimal;

/// Publishes Rentals' published language (`api.events`, integration-contracts.md) from its domain events: only what
/// other contexts listen to. `BikeUnlocked` leaves Rentals for technicians only (a rider's unlock is a Rental Session
/// Started, not a published fact); `ReservationExpired` and `UnlockFailed` for every holder (the consumer filters).
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(BikeEvent.RentalSessionEnded event) {
        var position = event.position();
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded(
            event.sessionId().value(), event.bikeId().value(), event.rider().id(), event.startedAt(), event.endedAt(),
            event.endReason().name(), event.stationId() == null ? null : event.stationId().value(),
            latitude(position), longitude(position)));
    }

    @EventListener
    public void on(BikeEvent.BikeWithdrawn event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn(
            event.bikeId().value(), event.reason().name(), event.withdrawnAt(), event.faultDescription()));
    }

    @EventListener
    public void on(BikeEvent.BikeUnlocked event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeUnlocked(
            event.bikeId().value(), event.holder().type().name(), event.holder().id(), event.unlockedAt()));
    }

    @EventListener
    public void on(BikeEvent.ReservationExpired event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.ReservationExpired(
            event.bikeId().value(), event.holder().type().name(), event.holder().id(), event.expiredAt()));
    }

    @EventListener
    public void on(BikeEvent.UnlockFailed event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.UnlockFailed(
            event.bikeId().value(), event.holder().type().name(), event.holder().id(), event.failedAt()));
    }

    private static BigDecimal latitude(Position position) {
        return position == null ? null : BigDecimal.valueOf(position.latitude());
    }

    private static BigDecimal longitude(Position position) {
        return position == null ? null : BigDecimal.valueOf(position.longitude());
    }
}
