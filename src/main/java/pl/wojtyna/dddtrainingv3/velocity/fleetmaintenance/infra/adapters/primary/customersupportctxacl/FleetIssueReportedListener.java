package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.customersupportctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.events.FleetIssueReported;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportBikeBroken;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportBikeBrokenUseCase;

/// Whenever Customer Support reports a fleet issue, the bike is broken at once (safety first, DE-52), the incident is
/// linked to it and, if it wasn't broken yet, Rentals is asked to withdraw it. Asynchronous, as from a message broker.
@PrimaryAdapter
public class FleetIssueReportedListener {

    private static final Logger log = LoggerFactory.getLogger(FleetIssueReportedListener.class);

    private final ReportBikeBrokenUseCase reportBikeBroken;

    public FleetIssueReportedListener(ReportBikeBrokenUseCase reportBikeBroken) {
        this.reportBikeBroken = reportBikeBroken;
    }

    @Async
    @EventListener
    public void on(FleetIssueReported event) {
        try {
            reportBikeBroken.handle(new ReportBikeBroken(event.bikeId(), "FLEET_ISSUE", event.description(), event.incidentId(),
                                                         event.reportedAt()));
        } catch (RuntimeException failure) {
            log.error("Registering bike {} of fleet issue {} as broken failed", event.bikeId(), event.incidentId(), failure);
            throw failure;
        }
    }
}
