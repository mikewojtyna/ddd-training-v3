package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffEvent.TariffPublished;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.launchPrices;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.launchTariff;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.t2;

@DisplayName("Tariff schedule examples")
class TariffScheduleTest {

    private static final PricingManagerId PAULA = new PricingManagerId("paula");

    // @formatter:off
    @DisplayName(
        """
         given it is 2026-11-08 10:00,
         when pricing manager Paula publishes T-2 valid from 2026-12-01 00:00,
         then T-2 is published and is listed among the tariffs with its valid from (Tariff Published)
        """
    )
    // @formatter:on
    @Test
    void completeTariffValidFromFutureMomentIsPublished() {
        // given
        var schedule = TariffSchedule.startingWith(launchTariff());
        var now = Instant.parse("2026-11-08T10:00:00Z");
        var tariff2 = t2("T-2", Instant.parse("2026-12-01T00:00:00Z"));

        // when
        var events = schedule.publish(tariff2).by(PAULA).at(now);

        // then
        assertThat(events.stream()).containsExactly(new TariffPublished(tariff2, PAULA, now));
        assertThat(schedule.tariffs()).contains(tariff2);
    }

    // @formatter:off
    @DisplayName(
        """
         given it is 2026-11-08 10:00,
         when Paula publishes tariff T-3 valid from 2026-11-08 10:00,
         then the publication is rejected, because its valid from isn't in the future (DE-96)
        """
    )
    // @formatter:on
    @Test
    void tariffValidFromNowIsRejected() {
        // given
        var schedule = TariffSchedule.startingWith(launchTariff());
        var now = Instant.parse("2026-11-08T10:00:00Z");

        // when
        var exception = catchThrowable(() -> schedule.publish(t2("T-3", now)).by(PAULA).at(now));

        // then
        assertThat(exception).isInstanceOf(ValidFromNotInFutureException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given it is 2026-11-08 10:00 and Anna's ride started at 09:30,
         when Paula publishes tariff T-3 valid from 2026-11-08 09:00,
         then the publication is rejected and Anna's ride keeps the launch tariff
        """
    )
    // @formatter:on
    @Test
    void retroactiveTariffIsRejectedAndStartedRideKeepsLaunchTariff() {
        // given
        var schedule = TariffSchedule.startingWith(launchTariff());
        var now = Instant.parse("2026-11-08T10:00:00Z");

        // when
        var exception = catchThrowable(() -> schedule.publish(t2("T-3", Instant.parse("2026-11-08T09:00:00Z")))
                                                     .by(PAULA)
                                                     .at(now));

        // then
        assertThat(exception).isInstanceOf(ValidFromNotInFutureException.class);
        assertThat(schedule.tariffs()).containsExactly(launchTariff());
    }

    // @formatter:off
    @DisplayName(
        """
         given T-2 was published valid from 2026-12-01 00:00,
         when on 2026-11-08 at 11:00 Paula publishes tariff T-5 valid from 2026-12-01 00:00,
         then the publication is rejected and T-2 stays the tariff valid from that moment
        """
    )
    // @formatter:on
    @Test
    void secondTariffWithSameValidFromIsRejected() {
        // given
        var schedule = TariffSchedule.startingWith(launchTariff());
        var december = Instant.parse("2026-12-01T00:00:00Z");
        var tariff2 = t2("T-2", december);
        schedule.publish(tariff2).by(PAULA).at(Instant.parse("2026-11-08T10:00:00Z"));

        // when
        var exception = catchThrowable(() -> schedule.publish(launchPrices("T-5", december))
                                                     .by(PAULA)
                                                     .at(Instant.parse("2026-11-08T11:00:00Z")));

        // then
        assertThat(exception).isInstanceOf(DuplicateValidFromException.class);
        assertThat(schedule.tariffs()).containsExactly(launchTariff(), tariff2);
    }

    // @formatter:off
    @DisplayName(
        """
         given T-2 was published valid from 2026-12-01 00:00,
         when Anna's ride, started on 2026-11-30 at 23:50 and ended with a station return on 2026-12-01 at 00:30, is priced,
         then the launch tariff applies
        """
    )
    // @formatter:on
    @Test
    void rideStartedBeforeNewTariffKeepsLaunchTariff() {
        // given
        var schedule = TariffSchedule.startingWith(launchTariff());
        schedule.publish(t2("T-2", Instant.parse("2026-12-01T00:00:00Z")))
                .by(PAULA)
                .at(Instant.parse("2026-11-08T10:00:00Z"));

        // when
        var tariff = schedule.tariffValidAt(Instant.parse("2026-11-30T23:50:00Z"));

        // then
        assertThat(tariff).isEqualTo(launchTariff());
    }

    // @formatter:off
    @DisplayName(
        """
         given T-2 was published valid from 2026-12-01 00:00,
         when Ben's ride, started on 2026-12-01 at 00:05 and ended with a station return at 00:40, is priced,
         then T-2 applies
        """
    )
    // @formatter:on
    @Test
    void rideStartedAfterNewTariffUsesIt() {
        // given
        var schedule = TariffSchedule.startingWith(launchTariff());
        var tariff2 = t2("T-2", Instant.parse("2026-12-01T00:00:00Z"));
        schedule.publish(tariff2).by(PAULA).at(Instant.parse("2026-11-08T10:00:00Z"));

        // when
        var tariff = schedule.tariffValidAt(Instant.parse("2026-12-01T00:05:00Z"));

        // then
        assertThat(tariff).isEqualTo(tariff2);
    }

    // @formatter:off
    @DisplayName(
        """
         given T-2 was published valid from 2026-12-01 00:00,
         when Paula publishes another tariff called T-2 valid from 2027-01-01 00:00,
         then the publication is rejected, because a tariff id is unique (spec data constraint)
        """
    )
    // @formatter:on
    @Test
    void secondTariffWithSameIdIsRejected() {
        // given
        var schedule = TariffSchedule.startingWith(launchTariff());
        var now = Instant.parse("2026-11-08T10:00:00Z");
        schedule.publish(t2("T-2", Instant.parse("2026-12-01T00:00:00Z"))).by(PAULA).at(now);

        // when
        var exception = catchThrowable(() -> schedule.publish(t2("T-2", Instant.parse("2027-01-01T00:00:00Z")))
                                                     .by(PAULA)
                                                     .at(now));

        // then
        assertThat(exception).isInstanceOf(DuplicateTariffIdException.class);
    }
}
