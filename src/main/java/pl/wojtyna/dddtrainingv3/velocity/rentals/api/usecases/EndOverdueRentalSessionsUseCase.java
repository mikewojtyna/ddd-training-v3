package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;

/// Fleet operations' clock: ends every session running longer than the maximum rental time (DE-20, DE-82). Each bike
/// with a running session decides itself whether it is overdue.
@PrimaryPort
public class EndOverdueRentalSessionsUseCase {

    private final BikeRepository bikes;
    private final BikeCommands commands;

    public EndOverdueRentalSessionsUseCase(BikeRepository bikes, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.commands = new BikeCommands(bikes, eventPublisher);
    }

    /// @return how many sessions ended
    public int handle(EndOverdueRentalSessions command) {
        var now = command.now();
        return commands.executeEach(bikes.withSessionStartedBefore(now), bike -> bike.endIfOverdue(now));
    }
}
