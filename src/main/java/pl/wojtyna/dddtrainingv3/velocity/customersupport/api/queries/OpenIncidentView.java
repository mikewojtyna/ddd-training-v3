package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries;

import java.time.Instant;

/// One open incident in the support agent's queue.
public record OpenIncidentView(String incidentId, String type, String category, String description, Instant reportedAt) {
}
