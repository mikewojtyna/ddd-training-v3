package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RiderStanding;

import java.time.Clock;
import java.time.Instant;

/// Reserve Bike. For a rider the use case gathers the rider's standing (Accounts' eligibility through the port, and
/// whether the rider holds another bike) and the bike decides; a technician reserves without position or eligibility.
@PrimaryPort
public class ReserveBikeUseCase {

    private final BikeCommands commands;
    private final BikeRepository bikes;
    private final RentalEligibility eligibility;
    private final Clock clock;

    public ReserveBikeUseCase(BikeRepository bikes, RentalEligibility eligibility, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new BikeCommands(bikes, eventPublisher);
        this.bikes = bikes;
        this.eligibility = eligibility;
        this.clock = clock;
    }

    public BikeReservation handle(ReserveBike command) {
        var holder = Translation.holder(command.holderType(), command.holderId());
        var now = clock.instant();
        var events = holder.isRider()
            ? reserveForRide(command, holder, now)
            : commands.execute(command.bikeId(), bike -> bike.reserveForService(holder).at(now));
        return events.stream()
                     .filter(BikeReserved.class::isInstance)
                     .map(BikeReserved.class::cast)
                     .findFirst()
                     .map(reserved -> new BikeReservation(reserved.bikeId().value(), holder.type().name(), holder.id(),
                                                          reserved.reservedAt(), reserved.reservedUntil()))
                     .orElseThrow();
    }

    private DomainEvents reserveForRide(ReserveBike command, Holder rider, Instant now) {
        var position = Translation.position(command.latitude(), command.longitude());
        var standing = new RiderStanding(eligibility.of(rider), bikes.holdsABike(rider, now));
        return commands.execute(command.bikeId(), bike -> bike.reserveForRide(rider).standingAt(position).given(standing).at(now));
    }
}
