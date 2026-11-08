package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Optional;

/// What happens to an incident.
public sealed interface IncidentEvent extends DomainEvent {

    record IncidentReported(IncidentId incidentId, RiderId riderId, Category category, IncidentType type,
                            Description description, Optional<BikeId> bike, Instant reportedAt) implements IncidentEvent {
    }

    /// A fleet issue is handed to Fleet Maintenance (DE-52); published as `api.events.FleetIssueReported`.
    record FleetIssueReported(IncidentId incidentId, BikeId bikeId, Description description, Instant reportedAt) implements IncidentEvent {
    }

    record IncidentPickedUp(IncidentId incidentId, AgentId agentId, Instant pickedUpAt) implements IncidentEvent {
    }

    record IncidentAccepted(IncidentId incidentId, AgentId agentId, Instant acceptedAt) implements IncidentEvent {
    }

    record IncidentRejected(IncidentId incidentId, RiderId riderId, AgentId agentId, RejectionReason reason,
                            Instant rejectedAt, Instant reopenableUntil) implements IncidentEvent {
    }

    record IncidentEscalated(IncidentId incidentId, AgentId agentId, Team team, Instant escalatedAt) implements IncidentEvent {
    }

    record IncidentReopened(IncidentId incidentId, RiderId riderId, AgentId assignedAgentId, Instant reopenedAt) implements IncidentEvent {
    }

    /// `agent` is empty for automatic resolutions (uncontested rejection, bike fixed); `team` is set for an
    /// escalated incident.
    record IncidentResolved(IncidentId incidentId, RiderId riderId, Optional<AgentId> agent, Resolution resolution,
                            Optional<Team> team, Instant resolvedAt) implements IncidentEvent {
    }
}
