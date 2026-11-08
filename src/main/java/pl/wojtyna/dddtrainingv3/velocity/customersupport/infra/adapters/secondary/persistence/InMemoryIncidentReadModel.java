package pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.secondary.persistence;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries.IncidentReadModel;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries.IncidentView;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries.OpenIncidentView;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentEvent;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Projection of the incident events into the views riders and agents query. Fed synchronously by the use cases'
/// published domain events, so a query right after a command sees its effect.
@SecondaryAdapter
public class InMemoryIncidentReadModel implements IncidentReadModel {

    private final Map<String, Row> rows = new ConcurrentHashMap<>();

    @EventListener
    public void on(IncidentEvent event) {
        switch (event) {
            case IncidentEvent.IncidentReported reported -> rows.put(reported.incidentId().value(), new Row(
                reported.incidentId().value(), reported.riderId().value(), reported.type().name(),
                reported.category().name(), reported.description().value(), reported.reportedAt(), IncidentStatus.OPEN,
                null, null, null, null, null, null));
            case IncidentEvent.FleetIssueReported fleetIssue -> {
            }
            case IncidentEvent.IncidentPickedUp pickedUp ->
                update(pickedUp.incidentId().value(), row -> row.with(IncidentStatus.PICKED_UP).assignedTo(pickedUp.agentId().value()));
            case IncidentEvent.IncidentAccepted accepted ->
                update(accepted.incidentId().value(), row -> row.with(IncidentStatus.ACCEPTED));
            case IncidentEvent.IncidentRejected rejected ->
                update(rejected.incidentId().value(), row -> row.with(IncidentStatus.REJECTED)
                                                                .rejected(rejected.reason().value(), rejected.rejectedAt(), rejected.reopenableUntil()));
            case IncidentEvent.IncidentEscalated escalated ->
                update(escalated.incidentId().value(), row -> row.with(IncidentStatus.ESCALATED).escalatedTo(escalated.team().name()));
            case IncidentEvent.IncidentReopened reopened ->
                update(reopened.incidentId().value(), row -> row.with(IncidentStatus.REOPENED));
            case IncidentEvent.IncidentResolved resolved ->
                update(resolved.incidentId().value(), row -> row.with(IncidentStatus.RESOLVED).resolvedWith(resolved.resolution().value()));
        }
    }

    private void update(String incidentId, java.util.function.UnaryOperator<Row> change) {
        rows.computeIfPresent(incidentId, (id, row) -> change.apply(row));
    }

    @Override
    public Optional<IncidentView> incident(String incidentId) {
        return Optional.ofNullable(rows.get(incidentId)).map(Row::view);
    }

    @Override
    public List<OpenIncidentView> openIncidents() {
        return rows.values().stream()
                   .filter(row -> row.status() == IncidentStatus.OPEN)
                   .sorted(Comparator.comparing(Row::reportedAt))
                   .map(row -> new OpenIncidentView(row.incidentId(), row.type(), row.category(), row.description(), row.reportedAt()))
                   .toList();
    }

    @Override
    public List<IncidentView> incidentsOf(String riderId) {
        return rows.values().stream()
                   .filter(row -> row.riderId().equals(riderId))
                   .sorted(Comparator.comparing(Row::reportedAt))
                   .map(Row::view)
                   .toList();
    }

    private record Row(String incidentId, String riderId, String type, String category, String description,
                       Instant reportedAt, IncidentStatus status, String assignedAgentId, String team, String resolution,
                       String rejectionReason, Instant rejectedAt, Instant reopenableUntil) {

        Row with(IncidentStatus newStatus) {
            return new Row(incidentId, riderId, type, category, description, reportedAt, newStatus, assignedAgentId, team,
                           resolution, rejectionReason, rejectedAt, reopenableUntil);
        }

        Row assignedTo(String agentId) {
            return new Row(incidentId, riderId, type, category, description, reportedAt, status, agentId, team,
                           resolution, rejectionReason, rejectedAt, reopenableUntil);
        }

        Row escalatedTo(String newTeam) {
            return new Row(incidentId, riderId, type, category, description, reportedAt, status, assignedAgentId, newTeam,
                           resolution, rejectionReason, rejectedAt, reopenableUntil);
        }

        Row resolvedWith(String newResolution) {
            return new Row(incidentId, riderId, type, category, description, reportedAt, status, assignedAgentId, team,
                           newResolution, rejectionReason, rejectedAt, reopenableUntil);
        }

        Row rejected(String reason, Instant at, Instant until) {
            return new Row(incidentId, riderId, type, category, description, reportedAt, status, assignedAgentId, team,
                           resolution, reason, at, until);
        }

        IncidentView view() {
            return new IncidentView(incidentId, riderId, type, status.name(), assignedAgentId, team, resolution,
                                    rejectionReason, rejectedAt, reopenableUntil);
        }
    }
}
