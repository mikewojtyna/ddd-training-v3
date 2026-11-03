package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

/// A technician returns a bike to circulation, which means it is fixed.
public record ReturnBikeToCirculation(String bikeId, String technicianId) {
}
