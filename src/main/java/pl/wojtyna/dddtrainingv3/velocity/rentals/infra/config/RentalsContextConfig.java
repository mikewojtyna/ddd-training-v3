package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStationHandler;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.RentBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReserveBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.WithdrawBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.BorrowingRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.BorrowerEligibility;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.InMemoryBikeStationRepo;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.InMemoryBorrowingRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.rideraccountsctxacl.RiderAccountsEligibilityAdapter;

import java.time.Clock;

/// Spring wiring for Rentals. Domain classes aren't Spring beans.
@Configuration
public class RentalsContextConfig {

    @Bean
    public BorrowerEligibility rentalsBorrowerEligibility() {
        return new RiderAccountsEligibilityAdapter();
    }

    @Bean
    public BikeStationRepo rentalsBikeStationRepo() {
        return new InMemoryBikeStationRepo();
    }

    @Bean
    public BorrowingRepository rentalsBorrowingRepository() {
        return new InMemoryBorrowingRepository();
    }

    /// "Now" for the rules with a time (reservation window); tests inject a fixed clock instead.
    @Bean
    public Clock rentalsClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ReserveBikeUseCase rentalsReserveBikeUseCase(BikeStationRepo rentalsBikeStationRepo,
                                                        BorrowingRepository rentalsBorrowingRepository,
                                                        BorrowerEligibility rentalsBorrowerEligibility,
                                                        Clock rentalsClock, DomainEventPublisher eventPublisher) {
        return new ReserveBikeUseCase(rentalsBikeStationRepo, rentalsBorrowingRepository, rentalsBorrowerEligibility,
                                      rentalsClock, eventPublisher);
    }

    @Bean
    public RentBikeUseCase rentalsRentBikeUseCase(BikeStationRepo rentalsBikeStationRepo,
                                                  BorrowingRepository rentalsBorrowingRepository,
                                                  Clock rentalsClock, DomainEventPublisher eventPublisher) {
        return new RentBikeUseCase(rentalsBikeStationRepo, rentalsBorrowingRepository, rentalsClock, eventPublisher);
    }

    @Bean
    public WithdrawBikeUseCase rentalsWithdrawBikeUseCase(BikeStationRepo rentalsBikeStationRepo,
                                                          DomainEventPublisher eventPublisher) {
        return new WithdrawBikeUseCase(rentalsBikeStationRepo, eventPublisher);
    }

    @Bean
    public AvailableBikesAtStationHandler rentalsAvailableBikesAtStationHandler(BikeStationRepo rentalsBikeStationRepo,
                                                                               Clock rentalsClock) {
        return new AvailableBikesAtStationHandler(rentalsBikeStationRepo, rentalsClock);
    }
}
