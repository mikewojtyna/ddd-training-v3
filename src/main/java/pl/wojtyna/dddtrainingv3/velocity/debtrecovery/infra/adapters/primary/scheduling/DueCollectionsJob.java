package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary.scheduling;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.AttemptDueCollections;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.AttemptDueCollectionsUseCase;

import java.time.Clock;

/// Collections' clock (DE-40): regularly makes the collection attempts that are due. Due-ness (at once, then every 72
/// hours to the minute) is decided against the clock; the job's interval only bounds how late an attempt is made.
/// Runs only when scheduling is on (`velocity.scheduling.enabled`, off in tests, which call the use case directly).
@PrimaryAdapter
public class DueCollectionsJob {

    private final AttemptDueCollectionsUseCase attemptDueCollections;
    private final Clock clock;

    public DueCollectionsJob(AttemptDueCollectionsUseCase attemptDueCollections, Clock clock) {
        this.attemptDueCollections = attemptDueCollections;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${velocity.debtrecovery.collection-attempt-delay-ms:60000}")
    public void attemptDueCollections() {
        attemptDueCollections.handle(new AttemptDueCollections(clock.instant()));
    }
}
