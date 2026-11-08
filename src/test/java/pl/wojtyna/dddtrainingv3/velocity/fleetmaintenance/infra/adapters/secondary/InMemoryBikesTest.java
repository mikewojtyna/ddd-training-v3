package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("In-memory bikes")
class InMemoryBikesTest {

    private final InMemoryBikes bikes = new InMemoryBikes();

    @Test
    @DisplayName("a saved broken bike is loaded as saved and shows in the broken bikes")
    void savedBikeIsLoadedAndListed() {
        // given
        var bike = Bike.unknown(new BikeId("B-1"));
        bike.reportFault(FaultReport.fromRentals(FaultSource.LOCK_FAILED, Optional.empty())).at(Instant.parse("2026-11-09T10:00:00Z"));

        // when
        bikes.save(bike);

        // then
        var loaded = bikes.load(new BikeId("B-1")).orElseThrow();
        assertThat(loaded.condition()).isEqualTo(BikeCondition.BROKEN);
        assertThat(loaded.version().value()).isEqualTo(1);
        assertThat(bikes.brokenBikes()).extracting("bikeId").contains("B-1");
        assertThat(bikes.bike("B-1").orElseThrow().faultSource()).isEqualTo("LOCK_FAILED");
    }

    @Test
    @DisplayName("saving a bike loaded at an outdated version is rejected")
    void concurrentModificationIsRejected() {
        // given
        var bike = Bike.unknown(new BikeId("B-2"));
        bike.reportFault(FaultReport.fromRentals(FaultSource.LOCK_FAILED, Optional.empty())).at(Instant.parse("2026-11-09T10:00:00Z"));
        bikes.save(bike);
        var first = bikes.load(new BikeId("B-2")).orElseThrow();
        var second = bikes.load(new BikeId("B-2")).orElseThrow();
        first.releaseReservationOf(new TechnicianId("tom")).at(Instant.parse("2026-11-09T10:05:00Z"));
        bikes.save(first);

        // when
        var thrown = catchThrowable(() -> bikes.save(second));

        // then
        assertThat(thrown).isInstanceOf(OptimisticLockingException.class);
    }
}
