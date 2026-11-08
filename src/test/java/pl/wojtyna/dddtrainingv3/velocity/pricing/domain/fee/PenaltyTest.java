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

/// Pricing prices the end time it is given: Rentals ends an overdue session at the 12-hour mark (DE-82), so the
/// session reaches Pricing already ending at started + 12 h.
@DisplayName("Penalty examples (finance-penalizes-overdue-rental)")
class PenaltyTest {

    private final FeeCalculator calculator = new FeeCalculator();
    private final TariffSchedule launchOnly = TariffSchedule.startingWith(launchTariff());

    // @formatter:off
    @DisplayName(
        """
         given Ben's ride on bike B-17 started at 08:00:00, ran past 20:00:00 and was ended automatically at the
         12-hour mark, 20:00:00, for exceeding the maximum rental time,
         when the ride is priced,
         then its hourly fees are PLN 96.00 (12 billing hours), its penalty is PLN 150.00 and its total fee is PLN 246.00
        """
    )
    // @formatter:on
    @Test
    void overdueRentalPaysTwelveHoursPlusPenalty() {
        // given
        var session = new Session(at("08:00:00"), at("20:00:00"), Ending.MAX_RENTAL_TIME);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.billingHours()).isEqualTo(12);
        assertThat(fee.hourlyFeesTotal()).isEqualTo(pln("96.00"));
        assertThat(fee.penalty()).isEqualTo(pln("150.00"));
        assertThat(fee.total()).isEqualTo(pln("246.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Ben's ride started at 08:00:00 and the overrun was noticed only at 20:20, so the ride was ended
         automatically at the 12-hour mark, 20:00:00,
         when the ride is priced,
         then its hourly fees are PLN 96.00 for the 12 hours (not PLN 105.00 for 13 billing hours up to 20:20),
         its penalty is PLN 150.00 and its total fee is PLN 246.00 (DE-82)
        """
    )
    // @formatter:on
    @Test
    void overdueRentalNoticedLateIsPricedToTwelveHourMark() {
        // given: Rentals ended the session at the 12-hour mark, not when the overrun was noticed
        var session = new Session(at("08:00:00"), at("20:00:00"), Ending.MAX_RENTAL_TIME);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.hourlyFeesTotal()).isEqualTo(pln("96.00"));
        assertThat(fee.penalty()).isEqualTo(pln("150.00"));
        assertThat(fee.total()).isEqualTo(pln("246.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Ben's ride started at 08:00:00 and was ended automatically at the 12-hour mark, 20:00:00,
         and B-17 last signalled its position at (52.2500, 21.1000), outside every drop zone,
         when the ride is priced,
         then its return fee is PLN 0.00 and its total fee is PLN 246.00
        """
    )
    // @formatter:on
    @Test
    void overdueRentalPaysNoReturnFee() {
        // given
        var session = new Session(at("08:00:00"), at("20:00:00"), Ending.MAX_RENTAL_TIME);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.of(Zone.UNAUTHORIZED_ZONE));

        // then
        assertThat(fee.returnFeeKind()).isEqualTo(ReturnFeeKind.NONE);
        assertThat(fee.returnFee()).isEqualTo(pln("0.00"));
        assertThat(fee.total()).isEqualTo(pln("246.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride on bike B-22 started at 08:00:00 and ended with a station return at Central at 20:00:00,
         when the ride is priced,
         then its hourly fees are PLN 96.00, there is no penalty and its total fee is PLN 96.00
        """
    )
    // @formatter:on
    @Test
    void rideOfExactlyTwelveHoursHasNoPenalty() {
        // given
        var session = new Session(at("08:00:00"), at("20:00:00"), Ending.STATION);

        // when
        var fee = calculator.calculate(session, launchOnly, Optional.empty());

        // then
        assertThat(fee.hourlyFeesTotal()).isEqualTo(pln("96.00"));
        assertThat(fee.penalty()).isEqualTo(pln("0.00"));
        assertThat(fee.total()).isEqualTo(pln("96.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given tariff T-2, valid from 12:00, with a penalty of PLN 200.00 and every other price as in the launch tariff,
         and Ben's ride started at 08:00:00 and was ended automatically at the 12-hour mark, 20:00:00,
         when the ride is priced,
         then its penalty is PLN 150.00 (launch tariff) and its total fee is PLN 246.00
        """
    )
    // @formatter:on
    @Test
    void penaltyFromTariffValidAtStart() {
        // given
        var schedule = TariffSchedule.startingWith(launchTariff());
        schedule.publish(new Tariff(new TariffId("T-2"), at("12:00"), Duration.ofMinutes(15), pln("3.00"),
                                    pln("5.00"), pln("7.00"), pln("9.00"), pln("2.00"), pln("40.00"), pln("200.00")))
                .by(new PricingManagerId("paula"))
                .at(Instant.parse("2026-11-08T10:00:00Z"));
        var session = new Session(at("08:00:00"), at("20:00:00"), Ending.MAX_RENTAL_TIME);

        // when
        var fee = calculator.calculate(session, schedule, Optional.empty());

        // then
        assertThat(fee.penalty()).isEqualTo(pln("150.00"));
        assertThat(fee.total()).isEqualTo(pln("246.00"));
    }
}
