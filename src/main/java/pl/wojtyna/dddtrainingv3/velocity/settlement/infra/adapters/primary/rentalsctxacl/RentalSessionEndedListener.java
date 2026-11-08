package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.ChargeRide;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.ChargeRideUseCase;

/// Whenever Rentals reports a rental session ended, the ride is settled (*Charge Ride*). Asynchronous, as from a
/// message broker; a repeated report charges nothing again (DE-95).
@PrimaryAdapter
public class RentalSessionEndedListener {

    private static final Logger log = LoggerFactory.getLogger(RentalSessionEndedListener.class);

    private final ChargeRideUseCase chargeRide;

    public RentalSessionEndedListener(ChargeRideUseCase chargeRide) {
        this.chargeRide = chargeRide;
    }

    @Async
    @EventListener
    public void on(RentalSessionEnded event) {
        try {
            chargeRide.handle(new ChargeRide(event.sessionId(), event.riderId(), event.startedAt(), event.endedAt(),
                                             event.endReason(), event.stationId(), event.latitude(), event.longitude()));
        } catch (RuntimeException failure) {
            log.error("Settling ride {} of rider {} failed", event.sessionId(), event.riderId(), failure);
            throw failure;
        }
    }
}
