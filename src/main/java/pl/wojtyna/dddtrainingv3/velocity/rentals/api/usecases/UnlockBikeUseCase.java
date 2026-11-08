package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionStarted;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.UnlockFailed;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeLocks;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility;

import java.time.Clock;
import java.time.Instant;

/// Unlock Bike. A rider's eligibility is asked again (DE-80) and passed to the bike, which opens the lock through
/// `BikeLocks` only when the unlock is allowed. A technician's unlock never asks Accounts and never starts a session.
@PrimaryPort
public class UnlockBikeUseCase {

    private final BikeCommands commands;
    private final RentalEligibility eligibility;
    private final BikeLocks locks;
    private final Clock clock;

    public UnlockBikeUseCase(BikeRepository bikes, RentalEligibility eligibility, BikeLocks locks,
                             DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new BikeCommands(bikes, eventPublisher);
        this.eligibility = eligibility;
        this.locks = locks;
        this.clock = clock;
    }

    public UnlockOutcome handle(UnlockBike command) {
        var holder = Translation.holder(command.holderType(), command.holderId());
        var now = clock.instant();
        var events = holder.isRider()
            ? unlockForRide(command, holder, now)
            : commands.execute(command.bikeId(), bike -> bike.unlockForService(holder).at(now).using(locks));
        if (events.hasOccurredEventOfType(UnlockFailed.class)) {
            return new UnlockOutcome(command.bikeId(), "UNLOCK_FAILED", null);
        }
        var sessionId = events.stream()
                              .filter(RentalSessionStarted.class::isInstance)
                              .map(RentalSessionStarted.class::cast)
                              .map(started -> started.sessionId().value())
                              .findFirst()
                              .orElse(null);
        return new UnlockOutcome(command.bikeId(), "UNLOCKED", sessionId);
    }

    private DomainEvents unlockForRide(UnlockBike command, Holder rider, Instant now) {
        var answer = eligibility.of(rider);
        return commands.execute(command.bikeId(), bike -> bike.unlockForRide(rider).given(answer).at(now).using(locks));
    }
}
