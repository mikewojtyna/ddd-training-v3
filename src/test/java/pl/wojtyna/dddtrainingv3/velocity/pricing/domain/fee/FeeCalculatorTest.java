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
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.launchTariff;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.pln;

@DisplayName("Fee calculation examples (finance-prices-ride-by-tiered-tariff)")
class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator();
    private final TariffSchedule launchOnly = TariffSchedule.startingWith(launchTariff());

    /// A time of day on 2026-11-09, the spec's example date, in UTC.
    static Instant at(String timeOfDay) {
        return Instant.parse("2026-11-09T" + (timeOfDay.length() == 5 ? timeOfDay + ":00" : timeOfDay) + "Z");
    }

    // @formatter:off
    @DisplayName(
        """
         given Ben's ride started at 10:00:00 and ended with a station return at Old Town at 10:15:01,
         when the ride is priced,
         then it has 1 billing hour and its total fee is PLN 3.00 (DE-83)
        """
    )
    // @formatter:on
    @Test
    void oneSecondAfterFreePeriodStartsFirstBillingHour() {
        // given
        var session = new Session(at("10:00:00"), at("10:15:01"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.billingHours()).isEqualTo(1);
        assertThat(fee.total()).isEqualTo(pln("3.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride on bike B-17 started at 10:00 and ended with a station return at Central at 10:12,
         when the ride is priced,
         then its hourly fees are PLN 0.00 and its total fee is PLN 0.00
        """
    )
    // @formatter:on
    @Test
    void rideOfTwelveMinutesIsFree() {
        // given
        var session = new Session(at("10:00"), at("10:12"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.hourlyFeesTotal()).isEqualTo(pln("0.00"));
        assertThat(fee.total()).isEqualTo(pln("0.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride started at 10:00:00 and ended with a station return at Central at 10:15:00 (exactly 15 minutes),
         when the ride is priced,
         then its total fee is PLN 0.00 (DE-31)
        """
    )
    // @formatter:on
    @Test
    void rideOfExactlyFifteenMinutesIsFree() {
        // given
        var session = new Session(at("10:00:00"), at("10:15:00"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.billingHours()).isZero();
        assertThat(fee.total()).isEqualTo(pln("0.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Ben's ride started at 10:00:00 and ended with a station return at Old Town at 11:15:00 (exactly 75 minutes),
         when the ride is priced,
         then it has 1 billing hour and its total fee is PLN 3.00
        """
    )
    // @formatter:on
    @Test
    void seventyFiveMinutesIsOneBillingHour() {
        // given
        var session = new Session(at("10:00:00"), at("11:15:00"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.billingHours()).isEqualTo(1);
        assertThat(fee.total()).isEqualTo(pln("3.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Ben's ride started at 10:00:00 and ended with a station return at Old Town at 11:15:01,
         when the ride is priced,
         then it has 2 billing hours, its hourly fees are PLN 3.00 + PLN 5.00 and its total fee is PLN 8.00
        """
    )
    // @formatter:on
    @Test
    void oneSecondMoreStartsSecondBillingHour() {
        // given
        var session = new Session(at("10:00:00"), at("11:15:01"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.hourlyFees()).containsExactly(pln("3.00"), pln("5.00"));
        assertThat(fee.total()).isEqualTo(pln("8.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride started at 10:00 and ended with a station return at Central at 13:15 (3 h after the free period),
         when the ride is priced,
         then it has 3 billing hours, its hourly fees are PLN 3.00 + 5.00 + 7.00 and its total fee is PLN 15.00
        """
    )
    // @formatter:on
    @Test
    void threeBillingHoursCostFifteen() {
        // given
        var session = new Session(at("10:00"), at("13:15"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.hourlyFees()).containsExactly(pln("3.00"), pln("5.00"), pln("7.00"));
        assertThat(fee.total()).isEqualTo(pln("15.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride started at 10:00 and ended with a station return at Central at 14:20 (4 h 5 min after the free period),
         when the ride is priced,
         then it has 5 billing hours, its hourly fees are PLN 3.00 + 5.00 + 7.00 + 9.00 + 9.00 and its total fee is PLN 33.00
        """
    )
    // @formatter:on
    @Test
    void fourthAndFurtherHoursCostTheSame() {
        // given
        var session = new Session(at("10:00"), at("14:20"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.hourlyFees()).containsExactly(pln("3.00"), pln("5.00"), pln("7.00"), pln("9.00"), pln("9.00"));
        assertThat(fee.total()).isEqualTo(pln("33.00"));
    }

    /// The launch tariff and T-2, valid from 2026-11-09 12:00, with hour 1 at PLN 4.00 and every other price as in
    /// the launch tariff.
    private static TariffSchedule launchAndT2WithFirstHourAtFour() {
        var schedule = TariffSchedule.startingWith(launchTariff());
        var tariff2 = new Tariff(new TariffId("T-2"), at("12:00"), Duration.ofMinutes(15), pln("4.00"), pln("5.00"),
                                 pln("7.00"), pln("9.00"), pln("2.00"), pln("40.00"), pln("150.00"));
        schedule.publish(tariff2).by(new PricingManagerId("paula")).at(Instant.parse("2026-11-08T10:00:00Z"));
        return schedule;
    }

    // @formatter:off
    @DisplayName(
        """
         given the launch tariff and tariff T-2, valid from 12:00, with hour 1 at PLN 4.00,
         and Ben's ride started at 11:50 and ended with a station return at Central at 12:30,
         when the ride is priced,
         then it is priced with the launch tariff and its total fee is PLN 3.00
        """
    )
    // @formatter:on
    @Test
    void rideStartedBeforeNewTariffUsesLaunchTariff() {
        // given
        var schedule = launchAndT2WithFirstHourAtFour();
        var session = new Session(at("11:50"), at("12:30"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, schedule, Optional.empty());

        // then
        assertThat(fee.tariffId()).isEqualTo(launchTariff().id());
        assertThat(fee.total()).isEqualTo(pln("3.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given the launch tariff and tariff T-2, valid from 12:00, with hour 1 at PLN 4.00,
         and Ben's ride started at 12:05 and ended with a station return at Central at 12:40,
         when the ride is priced,
         then it is priced with T-2 and its total fee is PLN 4.00
        """
    )
    // @formatter:on
    @Test
    void rideStartedAfterNewTariffUsesIt() {
        // given
        var schedule = launchAndT2WithFirstHourAtFour();
        var session = new Session(at("12:05"), at("12:40"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, schedule, Optional.empty());

        // then
        assertThat(fee.tariffId()).isEqualTo(new TariffId("T-2"));
        assertThat(fee.total()).isEqualTo(pln("4.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride started at 10:00 and ended with a station return at Old Town at 10:30,
         when the ride is priced,
         then its return fee is PLN 0.00 and its total fee is PLN 3.00
        """
    )
    // @formatter:on
    @Test
    void stationReturnPaysNoReturnFee() {
        // given
        var session = new Session(at("10:00"), at("10:30"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.returnFee()).isEqualTo(pln("0.00"));
        assertThat(fee.returnFeeKind()).isEqualTo(ReturnFeeKind.NONE);
        assertThat(fee.total()).isEqualTo(pln("3.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride started at 10:00 and ended in a lock failure at 10:30 at position (52.2500, 21.1000),
         outside every drop zone,
         when the ride is priced,
         then its return fee is PLN 0.00, there is no penalty and its total fee is PLN 3.00 (DE-23)
        """
    )
    // @formatter:on
    @Test
    void lockFailurePaysHourlyFeesOnly() {
        // given
        var session = new Session(at("10:00"), at("10:30"), Ending.LOCK_FAILURE);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.UNAUTHORIZED_ZONE));

        // then
        assertThat(fee.returnFee()).isEqualTo(pln("0.00"));
        assertThat(fee.penalty()).isEqualTo(pln("0.00"));
        assertThat(fee.total()).isEqualTo(pln("3.00"));
    }
}
