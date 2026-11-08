package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffSchedule;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffScheduleId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffScheduleRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Tariff schedules in memory, with optimistic locking through {@link Version}.
@SecondaryAdapter
public class InMemoryTariffScheduleRepository implements TariffScheduleRepository {

    private final Map<TariffScheduleId, TariffSchedule.Snapshot> schedules = new ConcurrentHashMap<>();

    @Override
    public Optional<TariffSchedule> load(TariffScheduleId id) {
        return Optional.ofNullable(schedules.get(id)).map(TariffSchedule::restore);
    }

    @Override
    public void save(TariffSchedule schedule) {
        schedules.compute(schedule.id(), (id, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(schedule.version())) {
                throw new OptimisticLockingException("Tariff schedule " + id.value() + " was modified concurrently");
            }
            var snapshot = schedule.snapshot();
            return new TariffSchedule.Snapshot(snapshot.id(), schedule.version().next(), snapshot.tariffs());
        });
    }
}
