package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.AttemptCollectionUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.CloseRecoveryOnDebtClearedUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.PauseRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.ResumeRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.StartRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.DebtCollector;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessRepo;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.repository.InMemoryRecoveryProcessRepo;

@Configuration
public class DebtRecoveryContextConfig {

    @Bean
    public RecoveryProcessRepo recoveryProcessRepo() {
        return new InMemoryRecoveryProcessRepo();
    }

    @Bean
    public StartRecoveryUseCase startRecoveryUseCase(RecoveryProcessRepo repo, DomainEventPublisher eventPublisher) {
        return new StartRecoveryUseCase(repo, eventPublisher);
    }

    @Bean
    public AttemptCollectionUseCase attemptCollectionUseCase(RecoveryProcessRepo repo, DebtCollector debtCollector,
                                                             DomainEventPublisher eventPublisher) {
        return new AttemptCollectionUseCase(repo, debtCollector, eventPublisher);
    }

    @Bean
    public PauseRecoveryUseCase pauseRecoveryUseCase(RecoveryProcessRepo repo, DomainEventPublisher eventPublisher) {
        return new PauseRecoveryUseCase(repo, eventPublisher);
    }

    @Bean
    public ResumeRecoveryUseCase resumeRecoveryUseCase(RecoveryProcessRepo repo, DomainEventPublisher eventPublisher) {
        return new ResumeRecoveryUseCase(repo, eventPublisher);
    }

    @Bean
    public CloseRecoveryOnDebtClearedUseCase closeRecoveryOnDebtClearedUseCase(RecoveryProcessRepo repo,
                                                                               DomainEventPublisher eventPublisher) {
        return new CloseRecoveryOnDebtClearedUseCase(repo, eventPublisher);
    }
}
