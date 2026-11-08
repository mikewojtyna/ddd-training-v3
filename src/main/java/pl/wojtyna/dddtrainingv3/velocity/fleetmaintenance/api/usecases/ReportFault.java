package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import java.time.Instant;

/// Canvas command *Report Fault* (technician app, DE-62).
public record ReportFault(String bikeId, String technicianId, String description, Instant reportedAt) {
}
