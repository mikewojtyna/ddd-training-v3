package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementNotFound;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;

@PrimaryPort
public class RecordChargeAttemptOutcomeUseCase {

    private final SettlementRepository settlements;
    private final DomainEventPublisher eventPublisher;

    public RecordChargeAttemptOutcomeUseCase(SettlementRepository settlements, DomainEventPublisher eventPublisher) {
        this.settlements = settlements;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ChargeAttemptOutcome outcome) {
        var rideId = new RideId(outcome.rideId());
        var settlement = settlements.load(rideId).orElseThrow(() -> new SettlementNotFound(rideId));
        var events = outcome.succeeded() ? settlement.chargeAttemptSucceeded() : settlement.chargeAttemptFailed();
        settlements.save(settlement);
        eventPublisher.publish(events);
    }
}
