package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.ReservationExpired;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReleaseTechnicianReservation;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReleaseTechnicianReservationUseCase;

/// Whenever a technician's reservation expires in Rentals, the broken bike waits for any technician again. Asynchronous, as from a message broker.
@PrimaryAdapter
public class ReservationExpiredListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiredListener.class);

    private final ReleaseTechnicianReservationUseCase releaseTechnicianReservation;

    public ReservationExpiredListener(ReleaseTechnicianReservationUseCase releaseTechnicianReservation) {
        this.releaseTechnicianReservation = releaseTechnicianReservation;
    }

    @Async
    @EventListener
    public void on(ReservationExpired event) {
        if (!"TECHNICIAN".equals(event.holderType())) {
            return;
        }
        try {
            releaseTechnicianReservation.handle(new ReleaseTechnicianReservation(event.bikeId(), event.holderId(), event.expiredAt()));
        } catch (RuntimeException failure) {
            log.error("Releasing the technician's reservation of bike {} failed", event.bikeId(), failure);
            throw failure;
        }
    }
}
