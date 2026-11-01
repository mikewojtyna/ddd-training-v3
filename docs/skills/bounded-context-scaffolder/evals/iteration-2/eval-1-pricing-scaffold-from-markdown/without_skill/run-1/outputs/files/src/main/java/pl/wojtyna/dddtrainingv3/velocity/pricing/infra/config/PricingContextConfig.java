package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFeeQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PriceRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishPriceListUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceCatalogRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.ReturnChargesProvider;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary.rentalsctxacl.RentalSessionEndedListener;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.events.RidePricedIntegrationEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.rentalsctxacl.RentalsReturnChargesProvider;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.repository.InMemoryPriceCatalogRepository;

import java.time.Clock;

@Configuration
public class PricingContextConfig {

    @Bean
    public PriceCatalogRepository priceCatalogRepository() {
        return new InMemoryPriceCatalogRepository();
    }

    @Bean
    public ReturnChargesProvider returnChargesProvider() {
        return new RentalsReturnChargesProvider();
    }

    @Bean
    public PublishPriceListUseCase publishPriceListUseCase(PriceCatalogRepository priceCatalogRepository,
                                                           DomainEventPublisher eventPublisher) {
        return new PublishPriceListUseCase(priceCatalogRepository, eventPublisher);
    }

    @Bean
    public PriceRideUseCase priceRideUseCase(PriceCatalogRepository priceCatalogRepository,
                                             ReturnChargesProvider returnChargesProvider,
                                             DomainEventPublisher eventPublisher) {
        return new PriceRideUseCase(priceCatalogRepository, returnChargesProvider, eventPublisher);
    }

    @Bean
    public EstimateFeeQueryHandler estimateFeeQueryHandler(PriceCatalogRepository priceCatalogRepository) {
        return new EstimateFeeQueryHandler(priceCatalogRepository, Clock.systemUTC());
    }

    @Bean
    public RentalSessionEndedListener rentalSessionEndedListener(PriceRideUseCase priceRideUseCase) {
        return new RentalSessionEndedListener(priceRideUseCase);
    }

    @Bean
    public RidePricedIntegrationEventPublisher ridePricedIntegrationEventPublisher(
        ApplicationEventPublisher applicationEventPublisher) {
        return new RidePricedIntegrationEventPublisher(applicationEventPublisher);
    }
}
