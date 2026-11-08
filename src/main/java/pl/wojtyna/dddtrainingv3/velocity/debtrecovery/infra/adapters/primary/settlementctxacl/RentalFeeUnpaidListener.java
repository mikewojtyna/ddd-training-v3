package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary.settlementctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.StartDebtRecovery;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.StartDebtRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.RentalFeeUnpaid;

/// Whenever Settlement reports a rental fee unpaid, debt recovery starts or the fee joins the rider's open recovery
/// (*Start Debt Recovery*). Asynchronous, as from a message broker.
@PrimaryAdapter
public class RentalFeeUnpaidListener {

    private static final Logger log = LoggerFactory.getLogger(RentalFeeUnpaidListener.class);

    private final StartDebtRecoveryUseCase startDebtRecovery;

    public RentalFeeUnpaidListener(StartDebtRecoveryUseCase startDebtRecovery) {
        this.startDebtRecovery = startDebtRecovery;
    }

    @Async
    @EventListener
    public void on(RentalFeeUnpaid event) {
        try {
            startDebtRecovery.handle(new StartDebtRecovery(event.riderId(), event.sessionId(), event.amount(),
                                                           event.outstandingDebt(), event.occurredAt()));
        } catch (RuntimeException failure) {
            log.error("Starting debt recovery for rider {} (ride {}) failed", event.riderId(), event.sessionId(), failure);
            throw failure;
        }
    }
}
