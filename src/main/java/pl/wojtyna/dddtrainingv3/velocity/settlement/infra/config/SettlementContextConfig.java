package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RideSettlementQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RiderBalanceQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.ChargeRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.CollectOutstandingDebtUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.PayOffOutstandingDebtUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedChargesUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCards;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.AccountBlocks;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.BlockRiderWithOutstandingDebtPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.UnblockRiderWhenDebtClearedPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.UnpaidRideFeeBecomesOutstandingDebtPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideFees;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.FailedChargeRetryJob;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.rentalsctxacl.RentalSessionEndedListener;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.InMemoryRideSettlementRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.InMemoryRiderDebtRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.IntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.accountsctxacl.AccountsAccountBlocks;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctxacl.PaymentsRiderCards;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.pricingctxacl.PricingRideFees;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.event.SettlementLocalEventsHandler;

import java.time.Clock;

/// Hand wiring of the Settlement context. Domain classes carry no Spring annotations.
@Configuration
public class SettlementContextConfig {

    // --- persistence ---

    @Bean
    public InMemoryRideSettlementRepository settlementRideSettlementRepository() {
        return new InMemoryRideSettlementRepository();
    }

    @Bean
    public InMemoryRiderDebtRepository settlementRiderDebtRepository() {
        return new InMemoryRiderDebtRepository();
    }

    // --- ACLs ---

    @Bean
    public RideFees settlementRideFees(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new PricingRideFees(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public RiderCards settlementRiderCards(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new PaymentsRiderCards(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public AccountBlocks settlementAccountBlocks(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new AccountsAccountBlocks(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public RentalSessionEndedListener settlementRentalSessionEndedListener(ChargeRideUseCase settlementChargeRideUseCase) {
        return new RentalSessionEndedListener(settlementChargeRideUseCase);
    }

    // --- use cases and queries ---

    @Bean
    public ChargeRideUseCase settlementChargeRideUseCase(InMemoryRideSettlementRepository settlementRideSettlementRepository,
                                                         RideFees settlementRideFees,
                                                         RiderCards settlementRiderCards,
                                                         DomainEventPublisher eventPublisher,
                                                         Clock clock) {
        return new ChargeRideUseCase(settlementRideSettlementRepository, settlementRideFees, settlementRiderCards,
                                     eventPublisher, clock);
    }

    @Bean
    public RetryFailedChargesUseCase settlementRetryFailedChargesUseCase(InMemoryRideSettlementRepository settlementRideSettlementRepository,
                                                                         RiderCards settlementRiderCards,
                                                                         DomainEventPublisher eventPublisher) {
        return new RetryFailedChargesUseCase(settlementRideSettlementRepository, settlementRiderCards, eventPublisher);
    }

    @Bean
    public PayOffOutstandingDebtUseCase settlementPayOffOutstandingDebtUseCase(InMemoryRiderDebtRepository settlementRiderDebtRepository,
                                                                               RiderCards settlementRiderCards,
                                                                               DomainEventPublisher eventPublisher,
                                                                               Clock clock) {
        return new PayOffOutstandingDebtUseCase(settlementRiderDebtRepository, settlementRiderCards, eventPublisher, clock);
    }

    @Bean
    public CollectOutstandingDebtUseCase settlementCollectOutstandingDebtUseCase(InMemoryRiderDebtRepository settlementRiderDebtRepository,
                                                                                 RiderCards settlementRiderCards,
                                                                                 DomainEventPublisher eventPublisher,
                                                                                 Clock clock) {
        return new CollectOutstandingDebtUseCase(settlementRiderDebtRepository, settlementRiderCards, eventPublisher, clock);
    }

    @Bean
    public RideSettlementQueryHandler settlementRideSettlementQueryHandler(InMemoryRideSettlementRepository settlementRideSettlementRepository) {
        return new RideSettlementQueryHandler(settlementRideSettlementRepository);
    }

    @Bean
    public RiderBalanceQueryHandler settlementRiderBalanceQueryHandler(InMemoryRiderDebtRepository settlementRiderDebtRepository) {
        return new RiderBalanceQueryHandler(settlementRiderDebtRepository);
    }

    @Bean
    public FailedChargeRetryJob settlementFailedChargeRetryJob(RetryFailedChargesUseCase settlementRetryFailedChargesUseCase,
                                                               Clock clock) {
        return new FailedChargeRetryJob(settlementRetryFailedChargesUseCase, clock);
    }

    // --- policies and publishing ---

    @Bean
    public SettlementLocalEventsHandler settlementLocalEventsHandler(InMemoryRiderDebtRepository settlementRiderDebtRepository,
                                                                     AccountBlocks settlementAccountBlocks,
                                                                     DomainEventPublisher eventPublisher) {
        return new SettlementLocalEventsHandler(
            new UnpaidRideFeeBecomesOutstandingDebtPolicy(settlementRiderDebtRepository, eventPublisher),
            new BlockRiderWithOutstandingDebtPolicy(settlementAccountBlocks),
            new UnblockRiderWhenDebtClearedPolicy(settlementAccountBlocks));
    }

    @Bean
    public IntegrationEventsPublisher settlementIntegrationEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new IntegrationEventsPublisher(applicationEventPublisher);
    }
}
