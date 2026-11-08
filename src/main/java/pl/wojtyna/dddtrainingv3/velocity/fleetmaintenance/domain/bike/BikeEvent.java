package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// What happens to a bike in Fleet Maintenance. None of these is published outside the context yet.
public sealed interface BikeEvent extends DomainEvent {

    BikeId bikeId();

    /// The bike became broken.
    record BikeReportedBroken(BikeId bikeId, FaultSource source, Optional<FaultDescription> description,
                              Optional<IncidentId> incident, Optional<TechnicianId> technician,
                              Instant reportedAt) implements BikeEvent {
    }

    /// Fleet Maintenance asks Rentals to take the bike away from riders (fleet issue, technician's report).
    record WithdrawalRequested(BikeId bikeId, Instant requestedAt) implements BikeEvent {
    }

    /// A fleet-issue incident about an already broken bike was linked to it.
    record IncidentLinked(BikeId bikeId, IncidentId incident, Instant linkedAt) implements BikeEvent {
    }

    /// Rentals reserved the broken bike for the technician until `reservedUntil`.
    record BikeReservedForTechnician(BikeId bikeId, TechnicianId technician, Instant reservedUntil) implements BikeEvent {
    }

    /// The technician's reservation ended without maintenance (expired or a failed unlock); the bike stays broken.
    record TechnicianReservationReleased(BikeId bikeId, TechnicianId technician, Instant releasedAt) implements BikeEvent {
    }

    record MaintenanceStarted(BikeId bikeId, TechnicianId technician, Instant startedAt) implements BikeEvent {
    }

    /// The bike is back in circulation; `incidents` are the linked fleet-issue incidents to resolve.
    record BikeFixed(BikeId bikeId, TechnicianId technician, Place place, Instant fixedAt,
                     List<IncidentId> incidents) implements BikeEvent {
    }
}
