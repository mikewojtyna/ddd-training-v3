package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStationQuery;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReturnBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.events.RentalsIntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.repository.InMemoryBikeStationRepo;

@Configuration
public class RentalsContextConfig {

    @Bean
    public BikeStationRepo bikeStationRepo() {
        return new InMemoryBikeStationRepo();
    }

    @Bean
    public ReturnBikeUseCase returnBikeUseCase(BikeStationRepo bikeStationRepo, DomainEventPublisher eventPublisher) {
        return new ReturnBikeUseCase(bikeStationRepo, eventPublisher);
    }

    @Bean
    public AvailableBikesAtStationQuery availableBikesAtStationQuery(BikeStationRepo bikeStationRepo) {
        return new AvailableBikesAtStationQuery(bikeStationRepo);
    }

    @Bean
    public RentalsIntegrationEventsPublisher rentalsIntegrationEventsPublisher(
        ApplicationEventPublisher applicationEventPublisher) {
        return new RentalsIntegrationEventsPublisher(applicationEventPublisher);
    }
}
