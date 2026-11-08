package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

/// Canvas command *Unlock Broken Bike* (technician app).
public record UnlockBrokenBike(String bikeId, String technicianId) {
}
