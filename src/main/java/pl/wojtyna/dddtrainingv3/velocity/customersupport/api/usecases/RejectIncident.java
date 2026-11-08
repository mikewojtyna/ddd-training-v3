package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Reject Incident* (support agent). The reason is required (DE-97).
public record RejectIncident(String incidentId, String agentId, String reason, Instant rejectedAt) {
}
