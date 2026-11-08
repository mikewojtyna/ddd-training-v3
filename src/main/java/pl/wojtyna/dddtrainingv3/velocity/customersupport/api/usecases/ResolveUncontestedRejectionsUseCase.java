package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;

/// Resolves every rejected incident whose reopening window has closed (DE-87). An incident changed concurrently (e.g.
/// reopened at the same moment) is skipped; the next run sees its new state (plan D4).
@PrimaryPort
public class ResolveUncontestedRejectionsUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;

    public ResolveUncontestedRejectionsUseCase(Incidents incidents, DomainEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
    }

    /// @return how many incidents were resolved
    public int handle(ResolveUncontestedRejections command) {
        var resolved = 0;
        for (var incident : incidents.rejected()) {
            var events = incident.resolveIfUncontestedAt(command.now());
            if (events.stream().findAny().isEmpty()) {
                continue;
            }
            try {
                incidents.save(incident);
            } catch (OptimisticLockingException concurrentChange) {
                continue;
            }
            eventPublisher.publish(events);
            resolved++;
        }
        return resolved;
    }
}
