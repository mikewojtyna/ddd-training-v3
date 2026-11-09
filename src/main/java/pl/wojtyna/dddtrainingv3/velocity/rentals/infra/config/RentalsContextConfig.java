package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.BikeAvailabilityQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.RidersActiveRentalQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.AddBikeToFleetUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessionsUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndRideHereUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.MakeBikeAvailableUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReserveBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReturnBikeAtStationUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.SignalFaultUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.UnlockBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.WithdrawBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Fleet;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Stations;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.primary.RentalsClockJobs;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.InMemoryBikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.IntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.accountsctxacl.AccountsRentalEligibilityAdapter;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.bikelocksacl.SimulatedBikeLocks;

import java.time.Clock;

/// Hand wiring of the Rentals context. Domain classes carry no Spring annotations.
@Configuration
public class RentalsContextConfig {

    @Bean
    public InMemoryBikeRepository rentalsBikeRepository() {
        return new InMemoryBikeRepository();
    }

    @Bean
    public Stations rentalsStations() {
        return Stations.reference();
    }

    @Bean
    public SimulatedBikeLocks rentalsBikeLocks() {
        return new SimulatedBikeLocks();
    }

    @Bean
    public RentalEligibility rentalsRentalEligibility(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new AccountsRentalEligibilityAdapter(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public IntegrationEventsPublisher rentalsIntegrationEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new IntegrationEventsPublisher(applicationEventPublisher);
    }

    @Bean
    public AddBikeToFleetUseCase rentalsAddBikeToFleetUseCase(InMemoryBikeRepository rentalsBikeRepository, Stations rentalsStations,
                                                              DomainEventPublisher eventPublisher, Clock clock) {
        return new AddBikeToFleetUseCase(new Fleet(rentalsBikeRepository, rentalsStations), rentalsBikeRepository, eventPublisher, clock);
    }

    @Bean
    public ReserveBikeUseCase rentalsReserveBikeUseCase(InMemoryBikeRepository rentalsBikeRepository,
                                                        RentalEligibility rentalsRentalEligibility,
                                                        DomainEventPublisher eventPublisher, Clock clock) {
        return new ReserveBikeUseCase(rentalsBikeRepository, rentalsRentalEligibility, eventPublisher, clock);
    }

    @Bean
    public UnlockBikeUseCase rentalsUnlockBikeUseCase(InMemoryBikeRepository rentalsBikeRepository,
                                                      RentalEligibility rentalsRentalEligibility, SimulatedBikeLocks rentalsBikeLocks,
                                                      DomainEventPublisher eventPublisher, Clock clock) {
        return new UnlockBikeUseCase(rentalsBikeRepository, rentalsRentalEligibility, rentalsBikeLocks, eventPublisher, clock);
    }

    @Bean
    public ReturnBikeAtStationUseCase rentalsReturnBikeAtStationUseCase(InMemoryBikeRepository rentalsBikeRepository,
                                                                        Stations rentalsStations, SimulatedBikeLocks rentalsBikeLocks,
                                                                        DomainEventPublisher eventPublisher, Clock clock) {
        return new ReturnBikeAtStationUseCase(rentalsBikeRepository, rentalsStations, rentalsBikeLocks, eventPublisher, clock);
    }

    @Bean
    public EndRideHereUseCase rentalsEndRideHereUseCase(InMemoryBikeRepository rentalsBikeRepository, SimulatedBikeLocks rentalsBikeLocks,
                                                        DomainEventPublisher eventPublisher, Clock clock) {
        return new EndRideHereUseCase(rentalsBikeRepository, rentalsBikeLocks, eventPublisher, clock);
    }

    @Bean
    public EndOverdueRentalSessionsUseCase rentalsEndOverdueRentalSessionsUseCase(InMemoryBikeRepository rentalsBikeRepository,
                                                                                  DomainEventPublisher eventPublisher) {
        return new EndOverdueRentalSessionsUseCase(rentalsBikeRepository, eventPublisher);
    }

    @Bean
    public SignalFaultUseCase rentalsSignalFaultUseCase(InMemoryBikeRepository rentalsBikeRepository,
                                                        DomainEventPublisher eventPublisher, Clock clock) {
        return new SignalFaultUseCase(rentalsBikeRepository, eventPublisher, clock);
    }

    @Bean
    public WithdrawBikeUseCase rentalsWithdrawBikeUseCase(InMemoryBikeRepository rentalsBikeRepository,
                                                          DomainEventPublisher eventPublisher, Clock clock) {
        return new WithdrawBikeUseCase(rentalsBikeRepository, eventPublisher, clock);
    }

    @Bean
    public MakeBikeAvailableUseCase rentalsMakeBikeAvailableUseCase(InMemoryBikeRepository rentalsBikeRepository, Stations rentalsStations,
                                                                    DomainEventPublisher eventPublisher, Clock clock) {
        return new MakeBikeAvailableUseCase(rentalsBikeRepository, rentalsStations, eventPublisher, clock);
    }

    @Bean
    public BikeAvailabilityQueryHandler rentalsBikeAvailabilityQueryHandler(InMemoryBikeRepository rentalsBikeRepository) {
        return new BikeAvailabilityQueryHandler(rentalsBikeRepository);
    }

    @Bean
    public RidersActiveRentalQueryHandler rentalsRidersActiveRentalQueryHandler(InMemoryBikeRepository rentalsBikeRepository, Clock clock) {
        return new RidersActiveRentalQueryHandler(rentalsBikeRepository, clock);
    }

    @Bean
    public RentalsClockJobs rentalsClockJobs(EndOverdueRentalSessionsUseCase rentalsEndOverdueRentalSessionsUseCase, Clock clock) {
        return new RentalsClockJobs(rentalsEndOverdueRentalSessionsUseCase, clock);
    }
}
