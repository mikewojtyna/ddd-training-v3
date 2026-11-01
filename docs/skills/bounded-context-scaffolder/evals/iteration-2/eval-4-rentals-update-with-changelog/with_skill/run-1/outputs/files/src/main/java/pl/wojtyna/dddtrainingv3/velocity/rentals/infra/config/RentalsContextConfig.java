package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStationQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.RentBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReturnBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.InMemoryAvailableBikesReadModel;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.InMemoryBikeStationRepo;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.IntegrationEventsPublisher;

@Configuration
public class RentalsContextConfig {

    @Bean
    public BikeStationRepo rentalsBikeStationRepo() {
        return new InMemoryBikeStationRepo();
    }

    @Bean
    public RentBikeUseCase rentalsRentBikeUseCase(BikeStationRepo rentalsBikeStationRepo,
                                                  DomainEventPublisher eventPublisher) {
        return new RentBikeUseCase(rentalsBikeStationRepo, eventPublisher);
    }

    @Bean
    public ReturnBikeUseCase rentalsReturnBikeUseCase(BikeStationRepo rentalsBikeStationRepo,
                                                      DomainEventPublisher eventPublisher) {
        return new ReturnBikeUseCase(rentalsBikeStationRepo, eventPublisher);
    }

    @Bean
    public InMemoryAvailableBikesReadModel rentalsAvailableBikesReadModel() {
        return new InMemoryAvailableBikesReadModel();
    }

    @Bean
    public AvailableBikesAtStationQueryHandler rentalsAvailableBikesAtStationQueryHandler(
        InMemoryAvailableBikesReadModel rentalsAvailableBikesReadModel) {
        return new AvailableBikesAtStationQueryHandler(rentalsAvailableBikesReadModel);
    }

    @Bean
    public IntegrationEventsPublisher rentalsIntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        return new IntegrationEventsPublisher(publisher);
    }
}
