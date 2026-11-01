package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFeeQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PriceRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishPriceListUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.PriceListsRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.ReturnChargesProvider;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.InMemoryPriceListsRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.IntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.rentalsctxacl.RentalsReturnChargesAdapter;

import java.time.Clock;

@Configuration
public class PricingContextConfig {

    @Bean
    public PriceListsRepository pricingPriceListsRepository() {
        return new InMemoryPriceListsRepository();
    }

    @Bean
    public ReturnChargesProvider pricingReturnChargesProvider() {
        return new RentalsReturnChargesAdapter();
    }

    @Bean
    public Clock pricingClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PublishPriceListUseCase pricingPublishPriceListUseCase(PriceListsRepository pricingPriceListsRepository,
                                                                  DomainEventPublisher eventPublisher) {
        return new PublishPriceListUseCase(pricingPriceListsRepository, eventPublisher);
    }

    @Bean
    public PriceRideUseCase pricingPriceRideUseCase(PriceListsRepository pricingPriceListsRepository,
                                                    ReturnChargesProvider pricingReturnChargesProvider,
                                                    DomainEventPublisher eventPublisher) {
        return new PriceRideUseCase(pricingPriceListsRepository, pricingReturnChargesProvider, eventPublisher);
    }

    @Bean
    public EstimateFeeQueryHandler pricingEstimateFeeQueryHandler(PriceListsRepository pricingPriceListsRepository,
                                                                  Clock pricingClock) {
        return new EstimateFeeQueryHandler(pricingPriceListsRepository, pricingClock);
    }

    @Bean
    public IntegrationEventsPublisher pricingIntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        return new IntegrationEventsPublisher(publisher);
    }
}
