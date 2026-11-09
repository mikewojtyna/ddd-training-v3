package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;

import java.time.Clock;

/// Cancel Reservation (spec rider-cancels-reservation).
@PrimaryPort
public class CancelReservationUseCase {

    private final BikeCommands commands;
    private final Clock clock;

    public CancelReservationUseCase(BikeRepository bikes, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new BikeCommands(bikes, eventPublisher);
        this.clock = clock;
    }

    public void handle(CancelReservation command) {
        var rider = Holder.rider(command.riderId());
        commands.execute(command.bikeId(), bike -> bike.cancelReservationOf(rider).at(clock.instant()));
    }
}
