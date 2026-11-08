package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.Optional;

@Repository
@SecondaryPort
public interface TariffScheduleRepository {

    Optional<TariffSchedule> load(TariffScheduleId id);

    /// Stores the schedule.
    ///
    /// @throws OptimisticLockingException if the stored version differs from {@code schedule.version()}
    void save(TariffSchedule schedule);
}
