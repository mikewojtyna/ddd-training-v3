package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.SettleRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RiderCharging;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.InMemorySettlementRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.IntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx.PaymentsRiderChargingAdapter;

@Configuration
public class SettlementContextConfig {

    @Bean
    public SettlementRepository settlementSettlementRepository() {
        return new InMemorySettlementRepository();
    }

    @Bean
    public RiderCharging settlementRiderCharging() {
        return new PaymentsRiderChargingAdapter();
    }

    @Bean
    public SettleRideUseCase settlementSettleRideUseCase(SettlementRepository settlementSettlementRepository,
                                                         RiderCharging settlementRiderCharging,
                                                         DomainEventPublisher eventPublisher) {
        return new SettleRideUseCase(settlementSettlementRepository, settlementRiderCharging, eventPublisher);
    }

    @Bean
    public IntegrationEventsPublisher settlementIntegrationEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new IntegrationEventsPublisher(applicationEventPublisher);
    }
}
