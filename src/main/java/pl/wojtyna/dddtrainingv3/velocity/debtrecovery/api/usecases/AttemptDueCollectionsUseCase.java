package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.DebtCollection;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.PaymentReminders;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcess;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcesses;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import java.time.Instant;

/// *Attempt Due Collections* (plan `collections-attempts-debt-collection` D7): for every running recovery, the
/// process decides whether an attempt is due (`attemptDueAt`); if so the rider is reminded (`PaymentReminders`),
/// Settlement is asked to collect the whole outstanding debt (`DebtCollection`), and the process records the outcome
/// (failed attempt, success or exhaustion) → save → publish. One rider's failure doesn't stop the others.
@PrimaryPort
public class AttemptDueCollectionsUseCase {

    private static final Logger log = LoggerFactory.getLogger(AttemptDueCollectionsUseCase.class);

    private final RecoveryProcesses processes;
    private final PaymentReminders reminders;
    private final DebtCollection debtCollection;
    private final RecoveryProcessChange change;

    public AttemptDueCollectionsUseCase(RecoveryProcesses processes, PaymentReminders reminders,
                                        DebtCollection debtCollection, DomainEventPublisher eventPublisher) {
        this.processes = processes;
        this.reminders = reminders;
        this.debtCollection = debtCollection;
        this.change = new RecoveryProcessChange(processes, eventPublisher);
    }

    public void handle(AttemptDueCollections command) {
        for (var candidate : processes.loadRunning()) {
            try {
                attemptIfDue(candidate.rider(), command.now());
            } catch (RuntimeException failure) {
                log.error("Collection attempt for rider {} failed", candidate.rider().value(), failure);
            }
        }
    }

    private void attemptIfDue(RiderId rider, Instant now) {
        var attempt = processes.load(rider).flatMap(process -> process.attemptDueAt(now));
        if (attempt.isEmpty()) {
            return;
        }
        reminders.remind(rider, attempt.get().amountAsked());
        var outcome = debtCollection.collect(rider);
        change.apply(rider, () -> RecoveryProcess.none(rider), process -> process.record(attempt.get()).as(outcome));
    }
}
