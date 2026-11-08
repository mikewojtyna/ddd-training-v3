package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;

/// Fleet operations' clock: releases every reservation whose period is over (DE-10, DE-11, DE-81).
@PrimaryPort
public class ReleaseExpiredReservationsUseCase {

    private final BikeRepository bikes;
    private final BikeCommands commands;

    public ReleaseExpiredReservationsUseCase(BikeRepository bikes, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.commands = new BikeCommands(bikes, eventPublisher);
    }

    /// @return how many reservations expired
    public int handle(ReleaseExpiredReservations command) {
        var now = command.now();
        return commands.executeEach(bikes.withReservationEndedBefore(now), bike -> bike.releaseIfExpired(now));
    }
}
