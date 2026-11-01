package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeRiderUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.AttemptChargeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryDueChargeAttemptsUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.SettleRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.ChargeRetryPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.ChargeRetryScheduler;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.RidePricedListener;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.events.DebtRecordedPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.repository.InMemorySettlementRepository;

import java.time.Clock;

@Configuration
@EnableScheduling
public class SettlementContextConfig {

    @Bean
    public ChargeRetryPolicy settlementChargeRetryPolicy() {
        return ChargeRetryPolicy.standard();
    }

    @Bean
    public Clock settlementClock() {
        return Clock.systemUTC();
    }

    @Bean
    public SettlementRepository settlementRepository() {
        return new InMemorySettlementRepository();
    }

    @Bean
    public AttemptChargeUseCase attemptChargeUseCase(SettlementRepository settlementRepository,
                                                     ChargeRiderUseCase payments,
                                                     DomainEventPublisher eventPublisher) {
        return new AttemptChargeUseCase(settlementRepository, payments, eventPublisher, settlementClock());
    }

    @Bean
    public SettleRideUseCase settleRideUseCase(SettlementRepository settlementRepository,
                                               AttemptChargeUseCase attemptChargeUseCase) {
        return new SettleRideUseCase(settlementRepository, attemptChargeUseCase, settlementChargeRetryPolicy());
    }

    @Bean
    public RetryDueChargeAttemptsUseCase retryDueChargeAttemptsUseCase(SettlementRepository settlementRepository,
                                                                       AttemptChargeUseCase attemptChargeUseCase) {
        return new RetryDueChargeAttemptsUseCase(settlementRepository, attemptChargeUseCase, settlementClock());
    }

    @Bean
    public RidePricedListener ridePricedListener(SettleRideUseCase settleRideUseCase) {
        return new RidePricedListener(settleRideUseCase);
    }

    @Bean
    public ChargeRetryScheduler chargeRetryScheduler(RetryDueChargeAttemptsUseCase retryDueChargeAttemptsUseCase) {
        return new ChargeRetryScheduler(retryDueChargeAttemptsUseCase);
    }

    @Bean
    public DebtRecordedPublisher debtRecordedPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DebtRecordedPublisher(applicationEventPublisher);
    }
}
