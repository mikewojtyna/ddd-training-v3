package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries.RecoveryProcessQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.AttemptDueCollectionsUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.PauseRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.RecordDebtClearedUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.ResumeRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.StartDebtRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.DebtCollection;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary.scheduling.DueCollectionsJob;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary.settlementctxacl.OutstandingDebtClearedListener;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary.settlementctxacl.RentalFeeUnpaidListener;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.messagingacl.SimulatedPaymentReminders;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.persistence.InMemoryRecoveryProcesses;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl.SettlementDebtCollection;

import java.time.Clock;

/// Hand wiring of the Debt Recovery context. Domain classes carry no Spring annotations.
@Configuration
public class DebtRecoveryContextConfig {

    // --- persistence ---

    @Bean
    public InMemoryRecoveryProcesses debtRecoveryRecoveryProcesses() {
        return new InMemoryRecoveryProcesses();
    }

    // --- ACLs and external parties ---

    @Bean
    public DebtCollection debtRecoveryDebtCollection(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new SettlementDebtCollection(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public SimulatedPaymentReminders debtRecoveryPaymentReminders() {
        return new SimulatedPaymentReminders();
    }

    @Bean
    public RentalFeeUnpaidListener debtRecoveryRentalFeeUnpaidListener(StartDebtRecoveryUseCase debtRecoveryStartDebtRecoveryUseCase) {
        return new RentalFeeUnpaidListener(debtRecoveryStartDebtRecoveryUseCase);
    }

    @Bean
    public OutstandingDebtClearedListener debtRecoveryOutstandingDebtClearedListener(RecordDebtClearedUseCase debtRecoveryRecordDebtClearedUseCase) {
        return new OutstandingDebtClearedListener(debtRecoveryRecordDebtClearedUseCase);
    }

    // --- use cases and queries ---

    @Bean
    public StartDebtRecoveryUseCase debtRecoveryStartDebtRecoveryUseCase(InMemoryRecoveryProcesses debtRecoveryRecoveryProcesses,
                                                                         DomainEventPublisher eventPublisher) {
        return new StartDebtRecoveryUseCase(debtRecoveryRecoveryProcesses, eventPublisher);
    }

    @Bean
    public AttemptDueCollectionsUseCase debtRecoveryAttemptDueCollectionsUseCase(InMemoryRecoveryProcesses debtRecoveryRecoveryProcesses,
                                                                                 SimulatedPaymentReminders debtRecoveryPaymentReminders,
                                                                                 DebtCollection debtRecoveryDebtCollection,
                                                                                 DomainEventPublisher eventPublisher) {
        return new AttemptDueCollectionsUseCase(debtRecoveryRecoveryProcesses, debtRecoveryPaymentReminders,
                                                debtRecoveryDebtCollection, eventPublisher);
    }

    @Bean
    public RecordDebtClearedUseCase debtRecoveryRecordDebtClearedUseCase(InMemoryRecoveryProcesses debtRecoveryRecoveryProcesses,
                                                                         DomainEventPublisher eventPublisher) {
        return new RecordDebtClearedUseCase(debtRecoveryRecoveryProcesses, eventPublisher);
    }

    @Bean
    public PauseRecoveryUseCase debtRecoveryPauseRecoveryUseCase(InMemoryRecoveryProcesses debtRecoveryRecoveryProcesses,
                                                                 DomainEventPublisher eventPublisher) {
        return new PauseRecoveryUseCase(debtRecoveryRecoveryProcesses, eventPublisher);
    }

    @Bean
    public ResumeRecoveryUseCase debtRecoveryResumeRecoveryUseCase(InMemoryRecoveryProcesses debtRecoveryRecoveryProcesses,
                                                                   DomainEventPublisher eventPublisher) {
        return new ResumeRecoveryUseCase(debtRecoveryRecoveryProcesses, eventPublisher);
    }

    @Bean
    public RecoveryProcessQueryHandler debtRecoveryRecoveryProcessQueryHandler(InMemoryRecoveryProcesses debtRecoveryRecoveryProcesses) {
        return new RecoveryProcessQueryHandler(debtRecoveryRecoveryProcesses);
    }

    @Bean
    public DueCollectionsJob debtRecoveryDueCollectionsJob(AttemptDueCollectionsUseCase debtRecoveryAttemptDueCollectionsUseCase,
                                                           Clock clock) {
        return new DueCollectionsJob(debtRecoveryAttemptDueCollectionsUseCase, clock);
    }
}
