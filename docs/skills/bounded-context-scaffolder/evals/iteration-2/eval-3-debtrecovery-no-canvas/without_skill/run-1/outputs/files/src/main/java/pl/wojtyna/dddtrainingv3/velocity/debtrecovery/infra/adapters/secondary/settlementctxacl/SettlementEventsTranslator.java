package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.CloseRecoveryOnDebtClearedUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.StartRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl.SettlementPublishedLanguage.OutstandingDebtCleared;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl.SettlementPublishedLanguage.RentalFeeUnpaid;

/**
 * Translates Settlement's events into Debt Recovery use cases.
 */
@SecondaryAdapter
public class SettlementEventsTranslator {

    private final StartRecoveryUseCase startRecoveryUseCase;
    private final CloseRecoveryOnDebtClearedUseCase closeRecoveryOnDebtClearedUseCase;

    public SettlementEventsTranslator(StartRecoveryUseCase startRecoveryUseCase,
                                      CloseRecoveryOnDebtClearedUseCase closeRecoveryOnDebtClearedUseCase) {
        this.startRecoveryUseCase = startRecoveryUseCase;
        this.closeRecoveryOnDebtClearedUseCase = closeRecoveryOnDebtClearedUseCase;
    }

    @EventListener
    public void handle(RentalFeeUnpaid event) {
        startRecoveryUseCase.startFor(new RiderId(event.riderId()));
    }

    @EventListener
    public void handle(OutstandingDebtCleared event) {
        closeRecoveryOnDebtClearedUseCase.debtClearedFor(new RiderId(event.riderId()));
    }
}
