package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Accept Incident* (support agent).
public record AcceptIncident(String incidentId, String agentId, Instant acceptedAt) {
}
