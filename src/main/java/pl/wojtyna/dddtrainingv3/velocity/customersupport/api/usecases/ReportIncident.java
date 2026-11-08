package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Report Incident* (rider app). `category` is one of `BROKEN_BIKE`, `CANT_UNLOCK`,
/// `WRONGFUL_CHARGE`, `SAFETY`, `OTHER`; `bikeId` is optional (`null` when the rider names no bike).
public record ReportIncident(String riderId, String category, String description, String bikeId, Instant reportedAt) {
}
