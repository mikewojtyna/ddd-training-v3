package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Charge;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeOutcome;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtEvent.OutstandingDebtCleared;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtEvent.RentalFeeUnpaid;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason.DECLINED;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason.NO_VALID_PAYMENT_METHOD;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln.pln;

/// Outstanding-debt examples of `docs/specs/finance-retries-failed-charge.md` (R2, R3) and
/// `docs/specs/rider-pays-off-outstanding-debt.md` (Settlement's part). All on 2026-11-09.
@DisplayName("Rider debt examples")
class RiderDebtTest {

    private static final RiderId BEN = new RiderId("ben");
    private static final SessionId S_3 = new SessionId("S-3");
    private static final SessionId S_5 = new SessionId("S-5");

    private static Instant at(String time) {
        return Instant.parse("2026-11-09T" + time + ":00Z");
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E2.2 given Ben owes nothing,
         when the 3rd attempt for S-3 (PLN 43.00) fails at 10:40,
         then S-3's fee is unpaid and Ben's outstanding debt is PLN 43.00 (Rental Fee Unpaid)
        """
    )
    // @formatter:on
    @Test
    void firstUnpaidFeeBecomesOutstandingDebt() {
        // given
        var debt = RiderDebt.none(BEN);

        // when
        var events = debt.addUnpaidFee(S_3).of(pln("43.00")).at(at("10:40"));

        // then
        assertThat(events.stream()).containsExactly(new RentalFeeUnpaid(BEN, S_3, pln("43.00"), pln("43.00"), at("10:40")));
        assertThat(debt.outstandingDebt()).isEqualTo(pln("43.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E3.1 given Ben owes PLN 43.00 from S-3,
         when the 3rd attempt for his ride S-5 (PLN 3.00) fails at 11:00,
         then S-5's fee is unpaid and Ben's outstanding debt is PLN 46.00 (Rental Fee Unpaid)
        """
    )
    // @formatter:on
    @Test
    void furtherUnpaidFeeAddsToOutstandingDebt() {
        // given
        var debt = owing43FromS3();

        // when
        var events = debt.addUnpaidFee(S_5).of(pln("3.00")).at(at("11:00"));

        // then
        assertThat(events.stream()).containsExactly(new RentalFeeUnpaid(BEN, S_5, pln("3.00"), pln("46.00"), at("11:00")));
    }

    // @formatter:off
    @DisplayName(
        """
         Payoff E1.1 given Anna owes PLN 43.00 from ride S-3,
         when Anna pays off her debt at 18:00 and the payment processor accepts PLN 43.00,
         then Anna owes PLN 0.00 (Outstanding Debt Cleared)
        """
    )
    // @formatter:on
    @Test
    void collectedDebtIsCleared() {
        // given
        var anna = new RiderId("anna");
        var debt = RiderDebt.none(anna);
        debt.addUnpaidFee(S_3).of(pln("43.00")).at(at("10:40"));
        var collection = debt.collection();

        // when
        var events = debt.record(collection).as(ChargeOutcome.success()).at(at("18:00"));

        // then
        assertThat(collection.amount()).isEqualTo(pln("43.00"));
        assertThat(events.stream()).containsExactly(new OutstandingDebtCleared(anna, pln("43.00"), at("18:00")));
        assertThat(debt.outstandingDebt()).isEqualTo(pln("0.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Payoff E1.2 given Ben owes PLN 46.00 from rides S-3 (PLN 43.00) and S-5 (PLN 3.00),
         when Ben pays off his debt and the payment processor accepts PLN 46.00,
         then one charge of PLN 46.00 is made and Ben owes PLN 0.00 (Outstanding Debt Cleared)
        """
    )
    // @formatter:on
    @Test
    void wholeDebtFromSeveralRidesIsCollectedInOneCharge() {
        // given
        var debt = owing43FromS3();
        debt.addUnpaidFee(S_5).of(pln("3.00")).at(at("11:00"));

        // when
        var collection = debt.collection();
        var events = debt.record(collection).as(ChargeOutcome.success()).at(at("18:00"));

        // then
        assertThat(collection.chargeAs("debt-1")).isEqualTo(new Charge(BEN, pln("46.00"), "debt-1"));
        assertThat(events.stream()).containsExactly(new OutstandingDebtCleared(BEN, pln("46.00"), at("18:00")));
        assertThat(debt.outstandingDebt()).isEqualTo(pln("0.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Payoff E2.1 given Anna owes PLN 43.00,
         when Anna pays off her debt and the payment processor declines PLN 43.00,
         then Anna still owes PLN 43.00
        """
    )
    // @formatter:on
    @Test
    void declinedPaymentLeavesDebt() {
        // given
        var debt = owing43FromS3();
        var collection = debt.collection();

        // when
        var events = debt.record(collection).as(ChargeOutcome.failure(DECLINED)).at(at("18:00"));

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(debt.outstandingDebt()).isEqualTo(pln("43.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Payoff E2.2 given Carla owes PLN 9.00 and has no current card,
         when Carla pays off her debt,
         then the payment fails and Carla still owes PLN 9.00
        """
    )
    // @formatter:on
    @Test
    void paymentWithoutValidCardLeavesDebt() {
        // given
        var carla = new RiderId("carla");
        var debt = RiderDebt.none(carla);
        debt.addUnpaidFee(new SessionId("S-6")).of(pln("3.00")).at(at("10:40"));
        debt.addUnpaidFee(new SessionId("S-7")).of(pln("3.00")).at(at("10:40"));
        debt.addUnpaidFee(new SessionId("S-8")).of(pln("3.00")).at(at("10:40"));
        var collection = debt.collection();

        // when
        var events = debt.record(collection).as(ChargeOutcome.failure(NO_VALID_PAYMENT_METHOD)).at(at("18:00"));

        // then
        assertThat(collection.amount()).isEqualTo(pln("9.00"));
        assertThat(events.stream()).isEmpty();
        assertThat(debt.outstandingDebt()).isEqualTo(pln("9.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Payoff E3.1 given Eva owes PLN 0.00,
         when Eva pays off her debt,
         then the payment is rejected
        """
    )
    // @formatter:on
    @Test
    void riderWithoutDebtHasNothingToPayOff() {
        // given
        var debt = RiderDebt.none(new RiderId("eva"));

        // when
        var thrown = catchThrowable(debt::collection);

        // then
        assertThat(thrown).isInstanceOf(NoOutstandingDebtException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         Payoff E1.3 (debt side) given Ben owes PLN 43.00 from S-3 and is asked for it,
         when his ride S-5 (PLN 3.00) becomes unpaid before the collection is recorded as charged,
         then only S-3 is cleared and Ben still owes PLN 3.00, without Outstanding Debt Cleared
        """
    )
    // @formatter:on
    @Test
    void collectionClearsOnlyTheRidesItCharged() {
        // given
        var debt = owing43FromS3();
        var collection = debt.collection();
        debt.addUnpaidFee(S_5).of(pln("3.00")).at(at("10:52"));

        // when
        var events = debt.record(collection).as(ChargeOutcome.success()).at(at("10:52"));

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(debt.outstandingDebt()).isEqualTo(pln("3.00"));
    }

    private static RiderDebt owing43FromS3() {
        var debt = RiderDebt.none(BEN);
        debt.addUnpaidFee(S_3).of(pln("43.00")).at(at("10:40"));
        return debt;
    }
}
