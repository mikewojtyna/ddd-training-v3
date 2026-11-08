package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@SecondaryPort
public interface BikeRepository extends FleetRegister {

    Optional<Bike> load(BikeId bikeId);

    /// @throws OptimisticLockingException if the stored version differs from `bike.version()` (for a new bike: if a
    ///                                    bike with the same id was added concurrently)
    void save(Bike bike);

    /// Bikes with a reservation whose end lies before `instant` (candidates for expiry).
    List<BikeId> withReservationEndedBefore(Instant instant);

    /// Bikes with a rental session that started before `instant` (candidates for the maximum rental time).
    List<BikeId> withSessionStartedBefore(Instant instant);

    /// Whether `holder` holds any bike at `now` (one rider, one bike, DE-13). A check across bikes: two concurrent
    /// reservations by the same rider can both pass it (accepted race, see the Rentals context plan).
    boolean holdsABike(Holder holder, Instant now);
}
