package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;

/// Read side of Fleet Maintenance's bikes (without their whereabouts), implemented next to the bike repository.
@SecondaryPort
public interface BrokenBikesReadModel {

    Optional<BrokenBikeView> bike(String bikeId);

    /// Bikes that are broken or under maintenance, earliest reported first.
    List<BrokenBikeView> brokenBikes();
}
