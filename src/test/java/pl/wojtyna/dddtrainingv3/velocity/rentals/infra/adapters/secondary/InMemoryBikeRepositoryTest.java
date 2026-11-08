package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Fleet;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Stations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryBikeRepositoryTest {

    private final InMemoryBikeRepository repository = new InMemoryBikeRepository();
    private final Fleet fleet = new Fleet(repository, Stations.reference());
    private final Instant now = Instant.parse("2026-11-08T10:00:00Z");

    @Test
    @DisplayName("a stale copy of a bike can't overwrite a newer save (optimistic locking)")
    void staleSaveIsRejected() {
        // given
        repository.save(fleet.add(new BikeId("B-1"), new StationId("Central"), now).bike());
        var first = repository.load(new BikeId("B-1")).orElseThrow();
        var second = repository.load(new BikeId("B-1")).orElseThrow();
        first.withdraw(now);
        repository.save(first);

        // when / then
        second.withdraw(now);
        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(OptimisticLockingException.class);
    }

    @Test
    @DisplayName("a second new bike with the same id is rejected even if both passed the fleet check")
    void secondNewBikeWithSameIdIsRejected() {
        // given
        var one = fleet.add(new BikeId("B-2"), new StationId("Central"), now).bike();
        var two = fleet.add(new BikeId("B-2"), new StationId("Old Town"), now).bike();
        repository.save(one);

        // when / then
        assertThatThrownBy(() -> repository.save(two)).isInstanceOf(OptimisticLockingException.class);
        assertThat(repository.bikeAvailability("B-2").orElseThrow().stationId()).isEqualTo("Central");
    }
}
