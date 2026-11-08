package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCards;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementRepository;

import java.time.Instant;

/// One charge attempt for a ride, shared by *Charge Ride* (attempt 1) and *Retry Failed Charges* (attempts 2 and 3):
/// if the ride says a charge is due, charge it through Payments → record the outcome → save → publish.
class RideChargeAttempt {

    private final RideSettlementRepository rides;
    private final RiderCards riderCards;
    private final DomainEventPublisher eventPublisher;

    RideChargeAttempt(RideSettlementRepository rides, RiderCards riderCards, DomainEventPublisher eventPublisher) {
        this.rides = rides;
        this.riderCards = riderCards;
        this.eventPublisher = eventPublisher;
    }

    void attempt(RideSettlement ride, Instant now) {
        ride.chargeDueAt(now).ifPresent(charge -> {
            var outcome = riderCards.charge(charge);
            var events = ride.recordCharge(outcome).at(now);
            rides.save(ride);
            eventPublisher.publish(events);
        });
    }
}
