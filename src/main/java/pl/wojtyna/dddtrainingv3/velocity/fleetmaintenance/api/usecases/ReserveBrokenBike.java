package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

/// Canvas command *Reserve Broken Bike* (technician app).
public record ReserveBrokenBike(String bikeId, String technicianId) {
}
