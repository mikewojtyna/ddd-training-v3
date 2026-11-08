package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCards;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.EndReason;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.FinishedRide;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideFees;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.time.Clock;
import java.util.Optional;

/// *Charge Ride* (plan `finance-charges-finished-ride` D5): ask Pricing for the fee → record the ride with its fee →
/// charge it through Payments unless it is free → record the outcome → save → publish.
///
/// A ride is recorded once (DE-95): a repeated "session ended" finds the ride and does nothing, and of two concurrent
/// deliveries only the first is saved (the repository refuses a second new ride), so only one charges.
@PrimaryPort
public class ChargeRideUseCase {

    private final RideSettlementRepository rides;
    private final RideFees rideFees;
    private final DomainEventPublisher eventPublisher;
    private final RideChargeAttempt chargeAttempt;
    private final Clock clock;

    public ChargeRideUseCase(RideSettlementRepository rides, RideFees rideFees, RiderCards riderCards,
                             DomainEventPublisher eventPublisher, Clock clock) {
        this.rides = rides;
        this.rideFees = rideFees;
        this.eventPublisher = eventPublisher;
        this.chargeAttempt = new RideChargeAttempt(rides, riderCards, eventPublisher);
        this.clock = clock;
    }

    public void handle(ChargeRide command) {
        var session = new SessionId(command.sessionId());
        if (rides.load(session).isPresent()) {
            return;
        }
        var finishedRide = new FinishedRide(session,
                                            new RiderId(command.riderId()),
                                            command.startedAt(),
                                            command.endedAt(),
                                            EndReason.valueOf(command.endReason()),
                                            Optional.ofNullable(command.stationId()),
                                            command.latitude() == null || command.longitude() == null
                                                ? Optional.empty()
                                                : Optional.of(new FinishedRide.Position(command.latitude(), command.longitude())));
        var fee = rideFees.feeOf(finishedRide);
        var now = clock.instant();
        var priced = RideSettlement.of(session).riddenBy(finishedRide.rider()).pricedAt(fee).at(now);
        try {
            rides.save(priced.ride());
        } catch (OptimisticLockingException alreadyRecorded) {
            return;
        }
        eventPublisher.publish(priced.events());
        rides.load(session).ifPresent(ride -> chargeAttempt.attempt(ride, now));
    }
}
