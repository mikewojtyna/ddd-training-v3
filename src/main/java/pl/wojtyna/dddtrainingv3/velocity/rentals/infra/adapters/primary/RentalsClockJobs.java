package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessions;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessionsUseCase;

import java.time.Clock;

/// Fleet operations' clock: every second it ends overdue sessions, with "now" from
/// `velocityClock`. Active only when scheduling is on (`VelocitySchedulingConfig`; off in tests, which call the use
/// cases themselves).
@PrimaryAdapter
public class RentalsClockJobs {

    private final EndOverdueRentalSessionsUseCase endOverdueRentalSessions;
    private final Clock clock;

    public RentalsClockJobs(EndOverdueRentalSessionsUseCase endOverdueRentalSessions, Clock clock) {
        this.endOverdueRentalSessions = endOverdueRentalSessions;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 1000)
    public void endOverdueRentalSessions() {
        endOverdueRentalSessions.handle(new EndOverdueRentalSessions(clock.instant()));
    }
}
