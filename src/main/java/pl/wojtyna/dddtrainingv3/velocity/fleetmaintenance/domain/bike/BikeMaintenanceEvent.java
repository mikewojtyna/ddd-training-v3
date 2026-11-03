package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

/// Facts recorded by the `Bike` aggregate. All of them are internal to Fleet Maintenance (plan D9):
/// other contexts learn about them only through the commands the policies send.
public sealed interface BikeMaintenanceEvent extends DomainEvent {

    BikeId bikeId();

    /// A bike in circulation was reported faulty and is now broken.
    record BikeReportedBroken(BikeId bikeId) implements BikeMaintenanceEvent {
    }

    /// A technician unlocked a broken bike.
    record MaintenanceStarted(BikeId bikeId, TechnicianId technicianId) implements BikeMaintenanceEvent {
    }

    /// A technician returned a bike under maintenance to circulation, which means it is fixed.
    record BikeFixed(BikeId bikeId, TechnicianId technicianId) implements BikeMaintenanceEvent {
    }
}
