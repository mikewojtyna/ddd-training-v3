package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.events;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.DebtRecorded;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementEvent;

/**
 * Turns the internal Debt Recorded domain event into the published {@link DebtRecorded} integration event for
 * DebtRecovery.
 */
@SecondaryAdapter
public class DebtRecordedPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DebtRecordedPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    public void on(SettlementEvent.DebtRecorded debtRecorded) {
        applicationEventPublisher.publishEvent(new DebtRecorded(debtRecorded.settlementId().value(),
                                                                debtRecorded.rideId().value(),
                                                                debtRecorded.riderId().value(),
                                                                debtRecorded.debt().amount(),
                                                                debtRecorded.recordedAt()));
    }
}
