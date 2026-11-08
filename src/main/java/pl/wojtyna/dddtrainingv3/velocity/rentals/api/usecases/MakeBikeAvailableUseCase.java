package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Stations;

import java.time.Clock;

/// Make Bike Available (Fleet Maintenance): a serviced bike back in circulation at a station or at a position.
@PrimaryPort
public class MakeBikeAvailableUseCase {

    private final BikeCommands commands;
    private final Stations stations;
    private final Clock clock;

    public MakeBikeAvailableUseCase(BikeRepository bikes, Stations stations, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new BikeCommands(bikes, eventPublisher);
        this.stations = stations;
        this.clock = clock;
    }

    public void handle(MakeBikeAvailable command) {
        var now = clock.instant();
        if (command.stationId() != null) {
            var station = stations.named(new StationId(command.stationId()));
            commands.execute(command.bikeId(), bike -> bike.makeAvailableAt(station).at(now));
        } else {
            var here = Translation.position(command.latitude(), command.longitude());
            commands.execute(command.bikeId(), bike -> bike.makeAvailableAt(here).at(now));
        }
    }
}
