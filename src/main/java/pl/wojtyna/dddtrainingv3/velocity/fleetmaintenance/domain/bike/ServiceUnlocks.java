package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Asks Rentals to open a reserved broken bike's lock for its technician (Rentals' *Unlock Bike*).
@SecondaryPort
public interface ServiceUnlocks {

    enum Outcome { UNLOCKED, UNLOCK_FAILED }

    /// @throws NotYourReservation if the technician doesn't hold an unexpired reservation of the bike
    Outcome unlock(BikeId bike, TechnicianId technician);
}
