package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.config;

import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RecordChargeAttemptOutcomeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.SettleRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCharger;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.InMemorySettlementRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.IntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx.PaymentsRiderCharger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;

@Configuration
public class SettlementContextConfig {

    @Bean
    public RiderCharger settlementRiderCharger() {
        return new PaymentsRiderCharger();
    }

    @Bean
    public SettlementRepository settlementSettlementRepository() {
        return new InMemorySettlementRepository();
    }

    @Bean
    public SettleRideUseCase settlementSettleRideUseCase(SettlementRepository settlementSettlementRepository,
                                                         RiderCharger settlementRiderCharger) {
        return new SettleRideUseCase(settlementSettlementRepository, settlementRiderCharger);
    }

    @Bean
    public RecordChargeAttemptOutcomeUseCase settlementRecordChargeAttemptOutcomeUseCase(
        SettlementRepository settlementSettlementRepository, DomainEventPublisher eventPublisher) {
        return new RecordChargeAttemptOutcomeUseCase(settlementSettlementRepository, eventPublisher);
    }

    @Bean
    public IntegrationEventsPublisher settlementIntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        return new IntegrationEventsPublisher(publisher);
    }
}
