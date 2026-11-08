package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Reopen Incident* (rider app).
public record ReopenIncident(String incidentId, String riderId, Instant reopenedAt) {
}
