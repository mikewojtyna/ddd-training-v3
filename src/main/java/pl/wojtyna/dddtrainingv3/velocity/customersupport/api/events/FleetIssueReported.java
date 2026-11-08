package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/// Published language: a rider reported an incident about a faulty bike (incident type: fleet issue).
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record FleetIssueReported(String incidentId, String bikeId, String description, Instant reportedAt) implements IntegrationEvent {
}
