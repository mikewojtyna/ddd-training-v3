package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.BikeAvailability;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.RentalsReadModel;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.RidersActiveRental;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Bike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Bike.Hold;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Position;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/// Bikes in memory, with optimistic locking on `Version`. Also serves Rentals' read model. Questions about a bike's
/// holds are answered by the restored `Bike` itself, so no rule is repeated here.
@SecondaryAdapter
public class InMemoryBikeRepository implements BikeRepository, RentalsReadModel {

    private final Map<BikeId, Bike.Snapshot> bikes = new ConcurrentHashMap<>();

    @Override
    public Optional<Bike> load(BikeId bikeId) {
        return Optional.ofNullable(bikes.get(bikeId)).map(Bike::restore);
    }

    @Override
    public void save(Bike bike) {
        var snapshot = bike.snapshot();
        bikes.compute(snapshot.id(), (bikeId, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(snapshot.version())) { // a stored bike is at least at version 1, so a second new bike fails too
                throw new OptimisticLockingException("Bike " + bikeId.value() + " was modified concurrently");
            }
            return snapshot.withVersion(snapshot.version().next());
        });
    }

    @Override
    public boolean contains(BikeId bike) {
        return bikes.containsKey(bike);
    }

    @Override
    public List<BikeId> withReservationEndedBefore(Instant instant) {
        return matching(bike -> bike.hasReservationEndedBefore(instant));
    }

    @Override
    public List<BikeId> withSessionStartedBefore(Instant instant) {
        return matching(bike -> bike.hasSessionStartedBefore(instant));
    }

    @Override
    public boolean holdsABike(Holder holder, Instant now) {
        return !matching(bike -> bike.isHeldBy(holder, now)).isEmpty();
    }

    private List<BikeId> matching(Predicate<Bike> predicate) {
        return bikes.values().stream().map(Bike::restore).filter(predicate).map(Bike::id).toList();
    }

    @Override
    public Optional<BikeAvailability> bikeAvailability(String bikeId) {
        return Optional.ofNullable(bikes.get(new BikeId(bikeId))).map(InMemoryBikeRepository::availabilityOf);
    }

    @Override
    public Optional<RidersActiveRental> activeRentalOf(String riderId, Instant now) {
        var rider = Holder.rider(riderId);
        return bikes.values().stream()
                    .filter(snapshot -> Bike.restore(snapshot).isHeldBy(rider, now))
                    .findFirst()
                    .map(snapshot -> switch (snapshot.hold()) {
                        case Hold.Reserved reserved -> new RidersActiveRental(riderId, snapshot.id().value(), "RESERVATION", null,
                                                                              reserved.reservedAt(), reserved.until());
                        case Hold.RentalSession session -> new RidersActiveRental(riderId, snapshot.id().value(), "SESSION",
                                                                                  session.sessionId().value(), session.startedAt(), null);
                        default -> throw new IllegalStateException("A rider holds " + snapshot.id().value() + " in an unknown way");
                    });
    }

    private static BikeAvailability availabilityOf(Bike.Snapshot snapshot) {
        var hold = switch (snapshot.hold()) {
            case Hold.Free free -> "FREE";
            case Hold.Reserved reserved -> "RESERVED";
            case Hold.RentalSession session -> "UNLOCKED";
            case Hold.ServiceUnlock unlock -> "UNLOCKED";
        };
        var holder = switch (snapshot.hold()) {
            case Hold.Free free -> null;
            case Hold.Reserved reserved -> reserved.holder();
            case Hold.RentalSession session -> session.rider();
            case Hold.ServiceUnlock unlock -> unlock.technician();
        };
        Position position = snapshot.position();
        return new BikeAvailability(snapshot.id().value(), snapshot.availability().name(), hold,
                                    holder == null ? null : holder.type().name(), holder == null ? null : holder.id(),
                                    position == null ? null : BigDecimal.valueOf(position.latitude()),
                                    position == null ? null : BigDecimal.valueOf(position.longitude()),
                                    snapshot.stationId() == null ? null : snapshot.stationId().value());
    }
}
