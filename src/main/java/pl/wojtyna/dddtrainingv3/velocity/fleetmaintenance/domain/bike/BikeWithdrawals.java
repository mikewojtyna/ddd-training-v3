package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Takes a broken bike away from riders at once (Rentals' *Withdraw Bike*); a ride on it goes on (DE-86).
@SecondaryPort
public interface BikeWithdrawals {

    /// @throws UnknownBike if the fleet has no such bike
    void withdraw(BikeId bike);
}
