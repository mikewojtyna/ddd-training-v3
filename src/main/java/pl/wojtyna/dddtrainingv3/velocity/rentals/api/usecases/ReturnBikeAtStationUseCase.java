package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeLocks;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Stations;

import java.time.Clock;

@PrimaryPort
public class ReturnBikeAtStationUseCase {

    private final BikeCommands commands;
    private final Stations stations;
    private final BikeLocks locks;
    private final Clock clock;

    public ReturnBikeAtStationUseCase(BikeRepository bikes, Stations stations, BikeLocks locks,
                                      DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new BikeCommands(bikes, eventPublisher);
        this.stations = stations;
        this.locks = locks;
        this.clock = clock;
    }

    public SessionEnd handle(ReturnBikeAtStation command) {
        var rider = Holder.rider(command.riderId());
        var station = stations.named(new StationId(command.stationId()));
        var now = clock.instant();
        var events = commands.execute(command.bikeId(), bike -> bike.returnAtStation(station)
                                                                    .by(rider)
                                                                    .reporting(command.faultDescription())
                                                                    .at(now)
                                                                    .using(locks));
        return SessionEnds.of(events);
    }

    /// The answer to the rider: how their session ended (vision: the rider is told why).
    static final class SessionEnds {

        private SessionEnds() {
        }

        static SessionEnd of(DomainEvents events) {
            return events.stream()
                         .filter(RentalSessionEnded.class::isInstance)
                         .map(RentalSessionEnded.class::cast)
                         .findFirst()
                         .map(ended -> new SessionEnd(ended.sessionId().value(), ended.bikeId().value(),
                                                      ended.endReason().name(), ended.endedAt()))
                         .orElseThrow();
        }
    }
}
