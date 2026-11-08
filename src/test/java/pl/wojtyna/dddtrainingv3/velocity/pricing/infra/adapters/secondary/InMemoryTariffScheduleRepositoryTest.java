package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.PricingManagerId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffSchedule;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.launchTariff;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.t2;

class InMemoryTariffScheduleRepositoryTest {

    private static final PricingManagerId PAULA = new PricingManagerId("paula");
    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    @Test
    @DisplayName("a stored schedule is loaded back with its tariffs")
    void storesAndLoadsSchedule() {
        // given
        var repository = new InMemoryTariffScheduleRepository();
        repository.save(TariffSchedule.startingWith(launchTariff()));

        // when
        var loaded = repository.load(TariffSchedule.VELOCITY).orElseThrow();

        // then
        assertThat(loaded.tariffs()).containsExactly(launchTariff());
    }

    @Test
    @DisplayName("of two copies loaded at the same version, only the first save wins")
    void rejectsStaleSave() {
        // given
        var repository = new InMemoryTariffScheduleRepository();
        repository.save(TariffSchedule.startingWith(launchTariff()));
        var first = repository.load(TariffSchedule.VELOCITY).orElseThrow();
        var second = repository.load(TariffSchedule.VELOCITY).orElseThrow();
        first.publish(t2("T-2", Instant.parse("2026-12-01T00:00:00Z"))).by(PAULA).at(NOW);
        second.publish(t2("T-3", Instant.parse("2027-01-01T00:00:00Z"))).by(PAULA).at(NOW);
        repository.save(first);

        // when
        var exception = catchThrowable(() -> repository.save(second));

        // then
        assertThat(exception).isInstanceOf(OptimisticLockingException.class);
        assertThat(repository.load(TariffSchedule.VELOCITY).orElseThrow().tariffs()).hasSize(2);
    }
}
