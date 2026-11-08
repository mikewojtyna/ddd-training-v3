package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeUnlocked;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.StartMaintenance;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.StartMaintenanceUseCase;

/// Whenever Rentals opens a bike's lock for a technician, maintenance on it starts. Asynchronous, as from a message broker.
@PrimaryAdapter
public class BikeUnlockedListener {

    private static final Logger log = LoggerFactory.getLogger(BikeUnlockedListener.class);

    private final StartMaintenanceUseCase startMaintenance;

    public BikeUnlockedListener(StartMaintenanceUseCase startMaintenance) {
        this.startMaintenance = startMaintenance;
    }

    @Async
    @EventListener
    public void on(BikeUnlocked event) {
        if (!"TECHNICIAN".equals(event.holderType())) {
            return;
        }
        try {
            startMaintenance.handle(new StartMaintenance(event.bikeId(), event.holderId(), event.unlockedAt()));
        } catch (RuntimeException failure) {
            log.error("Starting maintenance on bike {} failed", event.bikeId(), failure);
            throw failure;
        }
    }
}
