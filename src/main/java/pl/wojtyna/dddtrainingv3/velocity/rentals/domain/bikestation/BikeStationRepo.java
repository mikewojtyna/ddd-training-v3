package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

@SecondaryPort
public interface BikeStationRepo {

    Optional<BikeStation> load(BikeStationId id);

    /// The station that currently holds the bike, if any (Fleet Maintenance withdraws bikes by bike id only).
    Optional<BikeStation> loadContaining(BikeId bike);

    /// Saves the station. Throws `OptimisticLockingException` if it was saved by someone else since it was loaded
    /// (its `version()` no longer matches the stored one).
    void save(BikeStation bikeStation);
}
