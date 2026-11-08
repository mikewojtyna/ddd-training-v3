package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import java.time.Instant;

/// Canvas command *Report Bike Broken* (no REST; from Rentals' Bike Withdrawn and Customer Support's Fleet Issue
/// Reported). `faultSource` is one of `FAULT_REPORTED_AT_RETURN`, `LOCK_FAILED`, `FAULT_SIGNALLED`, `NOT_RETURNED`,
/// `FLEET_ISSUE`; `incidentId` is set for a fleet issue; `description` may be null.
public record ReportBikeBroken(String bikeId, String faultSource, String description, String incidentId, Instant reportedAt) {
}
