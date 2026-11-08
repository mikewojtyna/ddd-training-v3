package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessions;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessionsUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReleaseExpiredReservations;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReleaseExpiredReservationsUseCase;

import java.time.Clock;

/// Fleet operations' clock: every second it releases expired reservations and ends overdue sessions, with "now" from
/// `velocityClock`. Active only when scheduling is on (`VelocitySchedulingConfig`; off in tests, which call the use
/// cases themselves).
@PrimaryAdapter
public class RentalsClockJobs {

    private final ReleaseExpiredReservationsUseCase releaseExpiredReservations;
    private final EndOverdueRentalSessionsUseCase endOverdueRentalSessions;
    private final Clock clock;

    public RentalsClockJobs(ReleaseExpiredReservationsUseCase releaseExpiredReservations,
                            EndOverdueRentalSessionsUseCase endOverdueRentalSessions, Clock clock) {
        this.releaseExpiredReservations = releaseExpiredReservations;
        this.endOverdueRentalSessions = endOverdueRentalSessions;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 1000)
    public void releaseExpiredReservations() {
        releaseExpiredReservations.handle(new ReleaseExpiredReservations(clock.instant()));
    }

    @Scheduled(fixedDelay = 1000)
    public void endOverdueRentalSessions() {
        endOverdueRentalSessions.handle(new EndOverdueRentalSessions(clock.instant()));
    }
}
