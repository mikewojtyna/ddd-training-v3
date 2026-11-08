package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Resolve Incident* (support agent).
public record ResolveIncident(String incidentId, String agentId, String resolution, Instant resolvedAt) {
}
