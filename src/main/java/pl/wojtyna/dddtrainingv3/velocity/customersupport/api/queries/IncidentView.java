package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries;

import java.time.Instant;

/// One incident as riders and agents see it. Optional fields are `null` when they don't apply.
/// `status` is one of `OPEN`, `PICKED_UP`, `ACCEPTED`, `REJECTED`, `ESCALATED`, `REOPENED`, `RESOLVED`;
/// `reopenableUntil` is the first instant at which reopening is no longer possible (00:00 city time on the 15th day
/// after the rejection day).
public record IncidentView(String incidentId,
                           String riderId,
                           String type,
                           String status,
                           String assignedAgentId,
                           String team,
                           String resolution,
                           String rejectionReason,
                           Instant rejectedAt,
                           Instant reopenableUntil) {
}
