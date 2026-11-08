package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;

import java.time.Clock;

/// Withdraw Bike on a client's request (Fleet Maintenance), reason `REQUESTED`.
@PrimaryPort
public class WithdrawBikeUseCase {

    private final BikeCommands commands;
    private final Clock clock;

    public WithdrawBikeUseCase(BikeRepository bikes, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new BikeCommands(bikes, eventPublisher);
        this.clock = clock;
    }

    public void handle(WithdrawBike command) {
        var now = clock.instant();
        commands.execute(command.bikeId(), bike -> bike.withdraw(now));
    }
}
