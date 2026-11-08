package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Resolve Fleet Issue* (Fleet Maintenance, OHS): the incident's bike is fixed (DE-55).
public record ResolveFleetIssue(String incidentId, String bikeId, Instant resolvedAt) {
}
