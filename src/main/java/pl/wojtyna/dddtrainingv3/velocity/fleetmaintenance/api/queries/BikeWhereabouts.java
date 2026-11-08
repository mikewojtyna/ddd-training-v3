package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

/// Where a bike is, asked from Rentals' *Bike Availability* (Fleet Maintenance keeps no copy, plan
/// technician-reserves-broken-bike D6).
@SecondaryPort
public interface BikeWhereabouts {

    /// Empty when Rentals doesn't know the bike or doesn't answer.
    Optional<BrokenBikeView.Whereabouts> of(String bikeId);
}
