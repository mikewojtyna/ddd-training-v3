package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.CloseRecoveryOnDebtClearedUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.StartRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.DebtCollector;

@Configuration
public class SettlementContextAclConfig {

    @Bean
    public DebtCollector debtCollector() {
        return new SettlementDebtCollector();
    }

    @Bean
    public SettlementEventsTranslator settlementEventsTranslator(StartRecoveryUseCase startRecoveryUseCase,
                                                                 CloseRecoveryOnDebtClearedUseCase closeRecoveryOnDebtClearedUseCase) {
        return new SettlementEventsTranslator(startRecoveryUseCase, closeRecoveryOnDebtClearedUseCase);
    }
}
