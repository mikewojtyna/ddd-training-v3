package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Pick Up Incident* (support agent).
public record PickUpIncident(String incidentId, String agentId, Instant pickedUpAt) {
}
