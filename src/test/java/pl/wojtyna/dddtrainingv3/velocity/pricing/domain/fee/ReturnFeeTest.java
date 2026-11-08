package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.PricingManagerId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.Tariff;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffSchedule;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.Zone;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee.FeeCalculatorTest.at;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.launchTariff;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.pln;

/// The zone where a free-floating session ended is a fact given to the calculation: the query handler asks the
/// City's zone map for it (the position-to-zone geometry, border included, is tested in the `cityacl` adapter).
/// (52.2298, 21.0123) and (52.23104, 21.0122) lie in the Old Town Square drop zone, (52.2500, 21.1000) outside
/// every drop zone.
@DisplayName("Return fee examples (finance-prices-free-floating-return-by-zone)")
class ReturnFeeTest {

    private final FeeCalculator calculator = new FeeCalculator();
    private final TariffSchedule launchOnly = TariffSchedule.startingWith(launchTariff());

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride on bike B-17 started at 10:00 and ended with a free-floating return at (52.2298, 21.0123) at 10:30,
         when the ride is priced,
         then its hourly fees are PLN 3.00, its return fee is the drop zone fee PLN 2.00 and its total fee is PLN 5.00
        """
    )
    // @formatter:on
    @Test
    void returnInDropZoneAddsDropZoneFee() {
        // given
        var session = new Session(at("10:00"), at("10:30"), Ending.FREE_FLOATING);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.DROP_ZONE));

        // then
        assertThat(fee.hourlyFeesTotal()).isEqualTo(pln("3.00"));
        assertThat(fee.returnFeeKind()).isEqualTo(ReturnFeeKind.DROP_ZONE);
        assertThat(fee.returnFee()).isEqualTo(pln("2.00"));
        assertThat(fee.total()).isEqualTo(pln("5.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Ben's ride on bike B-22 started at 10:00 and ended with a free-floating return at (52.2500, 21.1000) at 10:30,
         when the ride is priced,
         then its hourly fees are PLN 3.00, its return fee is the unauthorized zone return fee PLN 40.00 and its total fee is PLN 43.00
        """
    )
    // @formatter:on
    @Test
    void returnOutsideDropZonesAddsUnauthorizedZoneFee() {
        // given
        var session = new Session(at("10:00"), at("10:30"), Ending.FREE_FLOATING);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.UNAUTHORIZED_ZONE));

        // then
        assertThat(fee.hourlyFeesTotal()).isEqualTo(pln("3.00"));
        assertThat(fee.returnFeeKind()).isEqualTo(ReturnFeeKind.UNAUTHORIZED_ZONE);
        assertThat(fee.returnFee()).isEqualTo(pln("40.00"));
        assertThat(fee.total()).isEqualTo(pln("43.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride started at 10:00 and ended with a free-floating return at (52.2298, 21.0123) at 10:10, inside the free period,
         when the ride is priced,
         then its hourly fees are PLN 0.00, its return fee is PLN 2.00 and its total fee is PLN 2.00 (DE-84)
        """
    )
    // @formatter:on
    @Test
    void quickReturnInDropZoneStillPaysDropZoneFee() {
        // given
        var session = new Session(at("10:00"), at("10:10"), Ending.FREE_FLOATING);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.DROP_ZONE));

        // then
        assertThat(fee.hourlyFeesTotal()).isEqualTo(pln("0.00"));
        assertThat(fee.returnFee()).isEqualTo(pln("2.00"));
        assertThat(fee.total()).isEqualTo(pln("2.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride started at 10:00 and ended with a free-floating return at 10:30 at (52.23104, 21.0122),
         149 m from the centre of the Old Town Square drop zone (radius 150 m), right at its border,
         when the ride is priced,
         then its return fee is the drop zone fee PLN 2.00 and its total fee is PLN 5.00 (DE-84)
        """
    )
    // @formatter:on
    @Test
    void returnOnDropZoneBorderPaysDropZoneFee() {
        // given: the zone map says the border position is inside the drop zone (see CityZoneMapAdapterTest)
        var session = new Session(at("10:00"), at("10:30"), Ending.FREE_FLOATING);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.DROP_ZONE));

        // then
        assertThat(fee.returnFee()).isEqualTo(pln("2.00"));
        assertThat(fee.total()).isEqualTo(pln("5.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given the Old Town Square drop zone was removed from the zone map at 10:20,
         and Anna's ride started at 10:00 and ended with a free-floating return at (52.2298, 21.0123) at 10:30,
         when the ride is priced,
         then the zone map valid at the ride's end applies, her return fee is the unauthorized zone return fee PLN 40.00
         and her total fee is PLN 43.00 (DE-84)
        """
    )
    // @formatter:on
    @Test
    void zoneMapValidAtRideEndApplies() {
        // given: the zone map valid at 10:30 has no drop zone at that position (see CityZoneMapAdapterTest)
        var session = new Session(at("10:00"), at("10:30"), Ending.FREE_FLOATING);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.UNAUTHORIZED_ZONE));

        // then
        assertThat(fee.returnFee()).isEqualTo(pln("40.00"));
        assertThat(fee.total()).isEqualTo(pln("43.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Ben's ride started at 10:00 and he ended it at (52.2500, 21.1000) at 10:30 but the lock didn't close,
         so it ended in a lock failure,
         when the ride is priced,
         then its return fee is PLN 0.00 and its total fee is PLN 3.00
        """
    )
    // @formatter:on
    @Test
    void lockFailureOutsideDropZonesPaysNoReturnFee() {
        // given
        var session = new Session(at("10:00"), at("10:30"), Ending.LOCK_FAILURE);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.UNAUTHORIZED_ZONE));

        // then
        assertThat(fee.returnFeeKind()).isEqualTo(ReturnFeeKind.NONE);
        assertThat(fee.returnFee()).isEqualTo(pln("0.00"));
        assertThat(fee.total()).isEqualTo(pln("3.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride started at 10:00 and ended in a lock failure at (52.2298, 21.0123) at 10:30,
         when the ride is priced,
         then no drop zone fee is added and its total fee is PLN 3.00
        """
    )
    // @formatter:on
    @Test
    void lockFailureInDropZonePaysNoDropZoneFee() {
        // given
        var session = new Session(at("10:00"), at("10:30"), Ending.LOCK_FAILURE);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.DROP_ZONE));

        // then
        assertThat(fee.returnFee()).isEqualTo(pln("0.00"));
        assertThat(fee.total()).isEqualTo(pln("3.00"));
    }

    /// The launch tariff and T-2, valid from 2026-11-09 12:00, with a drop zone fee of PLN 3.00 and every other price
    /// as in the launch tariff.
    private static TariffSchedule launchAndT2WithDropZoneFeeAtThree() {
        var schedule = TariffSchedule.startingWith(launchTariff());
        var tariff2 = new Tariff(new TariffId("T-2"), at("12:00"), Duration.ofMinutes(15), pln("3.00"), pln("5.00"),
                                 pln("7.00"), pln("9.00"), pln("3.00"), pln("40.00"), pln("150.00"));
        schedule.publish(tariff2).by(new PricingManagerId("paula")).at(Instant.parse("2026-11-08T10:00:00Z"));
        return schedule;
    }

    // @formatter:off
    @DisplayName(
        """
         given the launch tariff and tariff T-2, valid from 12:00, with a drop zone fee of PLN 3.00,
         and Anna's ride started at 11:50 and ended with a free-floating return at (52.2298, 21.0123) at 12:30,
         when the ride is priced,
         then its return fee is PLN 2.00 and its total fee is PLN 5.00
        """
    )
    // @formatter:on
    @Test
    void returnFeeFromLaunchTariffWhenStartedBeforeNewTariff() {
        // given
        var schedule = launchAndT2WithDropZoneFeeAtThree();
        var session = new Session(at("11:50"), at("12:30"), Ending.FREE_FLOATING);

        // when
        var fee = calculator.calculate(session, schedule, Optional.of(Zone.DROP_ZONE));

        // then
        assertThat(fee.returnFee()).isEqualTo(pln("2.00"));
        assertThat(fee.total()).isEqualTo(pln("5.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given the launch tariff and tariff T-2, valid from 12:00, with a drop zone fee of PLN 3.00,
         and Anna's ride started at 12:05 and ended with a free-floating return at (52.2298, 21.0123) at 12:40,
         when the ride is priced,
         then its return fee is PLN 3.00 and its total fee is PLN 6.00
        """
    )
    // @formatter:on
    @Test
    void returnFeeFromNewTariffWhenStartedAfterIt() {
        // given
        var schedule = launchAndT2WithDropZoneFeeAtThree();
        var session = new Session(at("12:05"), at("12:40"), Ending.FREE_FLOATING);

        // when
        var fee = calculator.calculate(session, schedule, Optional.of(Zone.DROP_ZONE));

        // then
        assertThat(fee.returnFee()).isEqualTo(pln("3.00"));
        assertThat(fee.total()).isEqualTo(pln("6.00"));
    }
}
