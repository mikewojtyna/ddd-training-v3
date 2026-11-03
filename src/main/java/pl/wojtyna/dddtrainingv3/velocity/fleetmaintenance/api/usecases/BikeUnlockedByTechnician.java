package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

/// Fleet Maintenance's own record of Rentals' *Bike Unlocked (by a technician)*.
public record BikeUnlockedByTechnician(String bikeId, String technicianId) {
}
