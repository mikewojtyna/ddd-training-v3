package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Category;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Description;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incident;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentReport;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.RiderId;

import java.util.Optional;
import java.util.UUID;

/// A rider reports an incident; its type is detected at once and a fleet issue is handed to Fleet Maintenance.
@PrimaryPort
public class ReportIncidentUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;

    public ReportIncidentUseCase(Incidents incidents, DomainEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
    }

    public IncidentReportReceipt handle(ReportIncident command) {
        var id = new IncidentId(UUID.randomUUID().toString());
        var report = new IncidentReport(new RiderId(command.riderId()),
                                        Category.valueOf(command.category()),
                                        new Description(command.description()),
                                        Optional.ofNullable(command.bikeId()).filter(bike -> !bike.isBlank()).map(BikeId::new));
        var reported = Incident.report(id, report, command.reportedAt());
        incidents.save(reported.incident());
        eventPublisher.publish(reported.events());
        return new IncidentReportReceipt(id.value(), reported.incident().type().name());
    }
}
