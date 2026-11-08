package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.Instant;

/// Asks Rentals to hold a broken bike for a technician (Rentals' *Reserve Bike for service*).
@SecondaryPort
public interface ServiceReservations {

    /// @return until when the bike is held for the technician
    /// @throws AlreadyReserved if someone else holds the bike
    /// @throws UnknownBike if the fleet has no such bike
    Instant reserve(BikeId bike, TechnicianId technician);
}
