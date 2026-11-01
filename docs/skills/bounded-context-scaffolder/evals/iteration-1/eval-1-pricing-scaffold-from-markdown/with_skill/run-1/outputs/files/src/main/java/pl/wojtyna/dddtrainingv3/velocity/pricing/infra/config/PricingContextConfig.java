package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFeeQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PriceRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishPriceListUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.ReturnChargesProvider;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.InMemoryPriceListRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.IntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.rentalsctxacl.RentalsReturnChargesAdapter;

import java.time.Clock;

@Configuration
public class PricingContextConfig {

    @Bean
    public PriceListRepository pricingPriceListRepository() {
        return new InMemoryPriceListRepository();
    }

    @Bean
    public ReturnChargesProvider pricingReturnChargesProvider() {
        return new RentalsReturnChargesAdapter();
    }

    @Bean
    public PublishPriceListUseCase pricingPublishPriceListUseCase(PriceListRepository pricingPriceListRepository) {
        return new PublishPriceListUseCase(pricingPriceListRepository);
    }

    @Bean
    public PriceRideUseCase pricingPriceRideUseCase(PriceListRepository pricingPriceListRepository,
                                                    ReturnChargesProvider pricingReturnChargesProvider,
                                                    DomainEventPublisher eventPublisher) {
        return new PriceRideUseCase(pricingPriceListRepository, pricingReturnChargesProvider, eventPublisher);
    }

    @Bean
    public EstimateFeeQueryHandler pricingEstimateFeeQueryHandler(PriceListRepository pricingPriceListRepository) {
        // Not a Clock bean on purpose: a context-wide Clock would be shared by every system in the application.
        return new EstimateFeeQueryHandler(pricingPriceListRepository, Clock.systemUTC());
    }

    @Bean
    public IntegrationEventsPublisher pricingIntegrationEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new IntegrationEventsPublisher(applicationEventPublisher);
    }
}
