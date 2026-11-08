package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeLocks;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;

import java.time.Clock;

/// End Ride Here (free-floating return). Rentals publishes the position; Pricing judges the zone.
@PrimaryPort
public class EndRideHereUseCase {

    private final BikeCommands commands;
    private final BikeLocks locks;
    private final Clock clock;

    public EndRideHereUseCase(BikeRepository bikes, BikeLocks locks, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new BikeCommands(bikes, eventPublisher);
        this.locks = locks;
        this.clock = clock;
    }

    public SessionEnd handle(EndRideHere command) {
        var rider = Holder.rider(command.riderId());
        var here = Translation.position(command.latitude(), command.longitude());
        var now = clock.instant();
        var events = commands.execute(command.bikeId(), bike -> bike.endRideHere(here).by(rider).at(now).using(locks));
        return ReturnBikeAtStationUseCase.SessionEnds.of(events);
    }
}
