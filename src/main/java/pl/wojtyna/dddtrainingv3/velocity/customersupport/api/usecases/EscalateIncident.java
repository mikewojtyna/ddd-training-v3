package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Escalate Incident* (support agent). `team` is one of `BILLING`, `FLEET`, `SAFETY` (DE-53).
public record EscalateIncident(String incidentId, String agentId, String team, Instant escalatedAt) {
}
