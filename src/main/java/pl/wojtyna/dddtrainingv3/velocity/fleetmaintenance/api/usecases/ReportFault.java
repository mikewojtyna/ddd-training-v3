package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

/// A report that a bike is broken: from a rider at return or the bike itself (via Rentals' *Bike Withdrawn*),
/// from a support incident, or from a technician.
public record ReportFault(String bikeId) {
}
