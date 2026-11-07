package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.BorrowerEligibility;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.rideraccountsctxacl.RiderAccountsEligibilityAdapter;

/// Spring wiring for Rentals. Domain classes aren't Spring beans; use cases, the repository and the clock are added
/// in plan step S8 (scaffolder 6b), once the domain exists.
@Configuration
public class RentalsContextConfig {

    @Bean
    public BorrowerEligibility rentalsBorrowerEligibility() {
        return new RiderAccountsEligibilityAdapter();
    }
}
