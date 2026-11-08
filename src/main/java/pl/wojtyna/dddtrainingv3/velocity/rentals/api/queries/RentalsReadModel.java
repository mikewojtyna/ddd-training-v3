package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.Instant;
import java.util.Optional;

@SecondaryPort
public interface RentalsReadModel {

    Optional<BikeAvailability> bikeAvailability(String bikeId);

    /// The reservation (that still holds at `now`) or running session of a rider, if any.
    Optional<RidersActiveRental> activeRentalOf(String riderId, Instant now);
}
