package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Charge;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeOutcome;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementEvent.ChargeAttemptFailed;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementEvent.RideFeeUnpaid;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementEvent.RideSettled;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason.DECLINED;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason.NO_VALID_PAYMENT_METHOD;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln.pln;

/// Examples of `docs/specs/finance-charges-finished-ride.md` and `docs/specs/finance-retries-failed-charge.md`
/// (Settlement's part: when and how often a ride is charged). All on 2026-11-09.
@DisplayName("Ride settlement examples")
class RideSettlementTest {

    private static final RiderId ANNA = new RiderId("anna");
    private static final SessionId S_1 = new SessionId("S-1");
    private static final RiderId BEN = new RiderId("ben");
    private static final SessionId S_3 = new SessionId("S-3");

    private static Instant at(String time) {
        return Instant.parse("2026-11-09T" + time + ":00Z");
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's ride S-1 ended at 10:30 and was priced at PLN 5.00,
         when S-1 is charged at 10:30 and the payment processor accepts PLN 5.00,
         then S-1 is settled (Ride Settled)
        """
    )
    // @formatter:on
    @Test
    void finishedRideChargedItsFeeIsSettled() {
        // given
        var ride = RideSettlement.of(S_1).riddenBy(ANNA).pricedAt(pln("5.00")).at(at("10:30")).ride();
        var charge = ride.chargeDueAt(at("10:30"));

        // when
        var events = ride.recordCharge(ChargeOutcome.success()).at(at("10:30"));

        // then
        assertThat(charge).contains(new Charge(ANNA, pln("5.00"), "S-1"));
        assertThat(events.stream()).containsExactly(new RideSettled(S_1, ANNA, pln("5.00"), true, at("10:30")));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna's ride S-2 started at 10:00, ended at 10:12 and was priced at PLN 0.00,
         when S-2 is charged,
         then S-2 is settled at once and Anna's card isn't charged (Ride Settled)
        """
    )
    // @formatter:on
    @Test
    void freeRideIsSettledWithoutCharging() {
        // given
        var s2 = new SessionId("S-2");

        // when
        var priced = RideSettlement.of(s2).riddenBy(ANNA).pricedAt(pln("0.00")).at(at("10:12"));

        // then
        assertThat(priced.events().stream()).containsExactly(new RideSettled(s2, ANNA, pln("0.00"), false, at("10:12")));
        assertThat(priced.ride().chargeDueAt(at("10:12"))).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Ben's ride S-3 ended at 10:30 and was priced at PLN 43.00,
         when S-3 is charged at 10:30 and the payment processor declines it,
         then S-3 isn't settled, this was attempt 1 of 3 and the next attempt is due at 10:35 (Charge Attempt Failed)
        """
    )
    // @formatter:on
    @Test
    void declinedFirstChargeSchedulesNextAttemptFiveMinutesLater() {
        // given
        var ride = RideSettlement.of(S_3).riddenBy(BEN).pricedAt(pln("43.00")).at(at("10:30")).ride();

        // when
        var events = ride.recordCharge(ChargeOutcome.failure(DECLINED)).at(at("10:30"));

        // then
        assertThat(events.stream()).containsExactly(
            new ChargeAttemptFailed(S_3, BEN, pln("43.00"), 1, DECLINED, at("10:30"), at("10:35")));
        assertThat(ride.chargeDueAt(at("10:30"))).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Carla has no current card and her ride S-4 ended at 10:30 and was priced at PLN 3.00,
         when S-4 is charged at 10:30,
         then the charge fails because Carla has no valid payment method, this was attempt 1 of 3
         and the next attempt is due at 10:35 (Charge Attempt Failed)
        """
    )
    // @formatter:on
    @Test
    void chargeWithoutValidPaymentMethodSchedulesNextAttempt() {
        // given
        var carla = new RiderId("carla");
        var s4 = new SessionId("S-4");
        var ride = RideSettlement.of(s4).riddenBy(carla).pricedAt(pln("3.00")).at(at("10:30")).ride();

        // when
        var events = ride.recordCharge(ChargeOutcome.failure(NO_VALID_PAYMENT_METHOD)).at(at("10:30"));

        // then
        assertThat(events.stream()).containsExactly(
            new ChargeAttemptFailed(s4, carla, pln("3.00"), 1, NO_VALID_PAYMENT_METHOD, at("10:30"), at("10:35")));
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna's ride S-1 is settled,
         when the news that S-1 ended arrives a second time,
         then Anna's card isn't charged again and S-1 stays settled
        """
    )
    // @formatter:on
    @Test
    void settledRideIsNotChargedAgain() {
        // given
        var ride = RideSettlement.of(S_1).riddenBy(ANNA).pricedAt(pln("5.00")).at(at("10:30")).ride();
        ride.recordCharge(ChargeOutcome.success()).at(at("10:30"));

        // when
        var charge = ride.chargeDueAt(at("10:31"));

        // then
        assertThat(charge).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E1.2 given attempt 1 for S-3 failed at 10:30,
         when failed charges are retried at 10:34,
         then no attempt is made for S-3
        """
    )
    // @formatter:on
    @Test
    void noRetryBeforeFiveMinutesPassed() {
        // given
        var ride = firstAttemptFailedAt1030();

        // when
        var charge = ride.chargeDueAt(at("10:34"));

        // then
        assertThat(charge).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E1.1 given Ben's ride S-3 is priced at PLN 43.00 and attempt 1 failed at 10:30,
         when failed charges are retried at 10:35 and the payment processor accepts PLN 43.00 on Ben's current card,
         then S-3 is settled (Ride Settled)
        """
    )
    // @formatter:on
    @Test
    void retryFiveMinutesLaterSettlesRide() {
        // given
        var ride = firstAttemptFailedAt1030();
        var charge = ride.chargeDueAt(at("10:35"));

        // when
        var events = ride.recordCharge(ChargeOutcome.success()).at(at("10:35"));

        // then
        assertThat(charge).contains(new Charge(BEN, pln("43.00"), "S-3"));
        assertThat(events.stream()).containsExactly(new RideSettled(S_3, BEN, pln("43.00"), true, at("10:35")));
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E2.1 given attempt 1 for Ben's ride S-3 (PLN 43.00) failed at 10:30,
         when failed charges are retried at 10:35 and the charge fails again,
         then this was attempt 2 of 3 and the next attempt is due at 10:40 (Charge Attempt Failed)
        """
    )
    // @formatter:on
    @Test
    void secondFailureSchedulesThirdAttempt() {
        // given
        var ride = firstAttemptFailedAt1030();

        // when
        var events = ride.recordCharge(ChargeOutcome.failure(DECLINED)).at(at("10:35"));

        // then
        assertThat(events.stream()).containsExactly(
            new ChargeAttemptFailed(S_3, BEN, pln("43.00"), 2, DECLINED, at("10:35"), at("10:40")));
        assertThat(ride.chargeDueAt(at("10:39"))).isEmpty();
        assertThat(ride.chargeDueAt(at("10:40"))).isPresent();
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E2.2 given attempts 1 and 2 for S-3 (PLN 43.00) failed at 10:30 and 10:35,
         when failed charges are retried at 10:40 and the charge fails again,
         then S-3's fee is unpaid and no further attempt is made
        """
    )
    // @formatter:on
    @Test
    void thirdFailureMakesFeeUnpaid() {
        // given
        var ride = firstAttemptFailedAt1030();
        ride.recordCharge(ChargeOutcome.failure(DECLINED)).at(at("10:35"));

        // when
        var events = ride.recordCharge(ChargeOutcome.failure(DECLINED)).at(at("10:40"));

        // then
        assertThat(events.stream()).containsExactly(new RideFeeUnpaid(S_3, BEN, pln("43.00"), DECLINED, at("10:40")));
        assertThat(ride.chargeDueAt(at("10:45"))).isEmpty();
    }

    private static RideSettlement firstAttemptFailedAt1030() {
        var ride = RideSettlement.of(S_3).riddenBy(BEN).pricedAt(pln("43.00")).at(at("10:30")).ride();
        ride.recordCharge(ChargeOutcome.failure(DECLINED)).at(at("10:30"));
        return ride;
    }
}
