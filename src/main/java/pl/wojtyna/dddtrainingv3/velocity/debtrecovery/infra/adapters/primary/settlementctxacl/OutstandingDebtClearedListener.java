package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary.settlementctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.RecordDebtCleared;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.RecordDebtClearedUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.OutstandingDebtCleared;

/// Whenever Settlement reports a rider's outstanding debt cleared (collected by Debt Recovery or paid in the app), the
/// rider's recovery succeeds (*Record Debt Cleared*; idempotent, DE-45). Asynchronous, as from a message broker.
@PrimaryAdapter
public class OutstandingDebtClearedListener {

    private static final Logger log = LoggerFactory.getLogger(OutstandingDebtClearedListener.class);

    private final RecordDebtClearedUseCase recordDebtCleared;

    public OutstandingDebtClearedListener(RecordDebtClearedUseCase recordDebtCleared) {
        this.recordDebtCleared = recordDebtCleared;
    }

    @Async
    @EventListener
    public void on(OutstandingDebtCleared event) {
        try {
            recordDebtCleared.handle(new RecordDebtCleared(event.riderId(), event.occurredAt()));
        } catch (RuntimeException failure) {
            log.error("Recording the cleared debt of rider {} failed", event.riderId(), failure);
            throw failure;
        }
    }
}
