package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;

import java.time.Clock;

/// Signal Fault, from the bike's telemetry. Accepted in every state (plan D7): withdrawn now, at the session end, or
/// nothing changes.
@PrimaryPort
public class SignalFaultUseCase {

    private final BikeCommands commands;
    private final Clock clock;

    public SignalFaultUseCase(BikeRepository bikes, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new BikeCommands(bikes, eventPublisher);
        this.clock = clock;
    }

    public void handle(SignalFault command) {
        if (command.faultCode() == null || command.faultCode().isBlank()) {
            throw new IllegalArgumentException("faultCode is required");
        }
        var now = clock.instant();
        commands.execute(command.bikeId(), bike -> bike.signalFault(command.faultCode()).at(now));
    }
}
