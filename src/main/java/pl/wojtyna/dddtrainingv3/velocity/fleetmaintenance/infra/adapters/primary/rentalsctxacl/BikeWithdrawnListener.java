package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportBikeBroken;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportBikeBrokenUseCase;

/// Whenever Rentals withdraws a bike because of a fault (lock failed, fault reported at return, fault signalled, not
/// returned), the bike is reported broken. Asynchronous, as from a message broker.
@PrimaryAdapter
public class BikeWithdrawnListener {

    private static final Logger log = LoggerFactory.getLogger(BikeWithdrawnListener.class);

    private final ReportBikeBrokenUseCase reportBikeBroken;

    public BikeWithdrawnListener(ReportBikeBrokenUseCase reportBikeBroken) {
        this.reportBikeBroken = reportBikeBroken;
    }

    @Async
    @EventListener
    public void on(BikeWithdrawn event) {
        var source = switch (event.reason()) {
            case "LOCK_FAILED", "FAULT_REPORTED_AT_RETURN", "FAULT_SIGNALLED", "NOT_RETURNED" -> event.reason();
            default -> null; // REQUESTED: Fleet Maintenance asked for it itself, not a new fault report
        };
        if (source == null) {
            return;
        }
        try {
            reportBikeBroken.handle(new ReportBikeBroken(event.bikeId(), source, event.faultDescription(), null, event.withdrawnAt()));
        } catch (RuntimeException failure) {
            log.error("Registering withdrawn bike {} as broken failed", event.bikeId(), failure);
            throw failure;
        }
    }
}
