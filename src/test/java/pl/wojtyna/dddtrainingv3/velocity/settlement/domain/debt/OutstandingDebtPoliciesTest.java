package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtEvent.OutstandingDebtCleared;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtEvent.RentalFeeUnpaid;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementEvent.RideFeeUnpaid;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.InMemoryRiderDebtRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason.DECLINED;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln.pln;

/// The policies around the outstanding debt (`finance-retries-failed-charge` R2, R3;
/// `rider-pays-off-outstanding-debt` R1). All on 2026-11-09.
@DisplayName("Outstanding debt policies")
class OutstandingDebtPoliciesTest {

    private static final RiderId BEN = new RiderId("ben");
    private static final SessionId S_3 = new SessionId("S-3");
    private static final SessionId S_5 = new SessionId("S-5");

    private final InMemoryRiderDebtRepository debts = new InMemoryRiderDebtRepository();
    private final RecordingEventPublisher publisher = new RecordingEventPublisher();
    private final RecordingAccountBlocks accountBlocks = new RecordingAccountBlocks();

    private static Instant at(String time) {
        return Instant.parse("2026-11-09T" + time + ":00Z");
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E2.2 given Ben owes nothing,
         when S-3's fee (PLN 43.00) stays unpaid after the 3rd attempt at 10:40,
         then Ben's outstanding debt is PLN 43.00 (Rental Fee Unpaid)
        """
    )
    // @formatter:on
    @Test
    void unpaidRideFeeBecomesOutstandingDebt() {
        // given
        var policy = new UnpaidRideFeeBecomesOutstandingDebtPolicy(debts, publisher);

        // when
        policy.handle(new RideFeeUnpaid(S_3, BEN, pln("43.00"), DECLINED, at("10:40")));

        // then
        assertThat(publisher.published).containsExactly(new RentalFeeUnpaid(BEN, S_3, pln("43.00"), pln("43.00"), at("10:40")));
        assertThat(debts.load(BEN)).hasValueSatisfying(debt -> assertThat(debt.outstandingDebt()).isEqualTo(pln("43.00")));
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E3.1 given Ben owes PLN 43.00 from S-3,
         when S-5's fee (PLN 3.00) stays unpaid after the 3rd attempt at 11:00,
         then Ben's outstanding debt is PLN 46.00 (Rental Fee Unpaid)
        """
    )
    // @formatter:on
    @Test
    void furtherUnpaidRideFeeAddsToStoredDebt() {
        // given
        var policy = new UnpaidRideFeeBecomesOutstandingDebtPolicy(debts, publisher);
        policy.handle(new RideFeeUnpaid(S_3, BEN, pln("43.00"), DECLINED, at("10:40")));

        // when
        policy.handle(new RideFeeUnpaid(S_5, BEN, pln("3.00"), DECLINED, at("11:00")));

        // then
        assertThat(publisher.published).last().isEqualTo(new RentalFeeUnpaid(BEN, S_5, pln("3.00"), pln("46.00"), at("11:00")));
    }

    // @formatter:off
    @DisplayName(
        """
         Retries E2.2 / E3.1 given a rental fee of Ben became outstanding debt (Rental Fee Unpaid),
         when the policy handles it,
         then Accounts is asked to block Ben for outstanding debt, every time (Accounts ignores a block in place, DE-94)
        """
    )
    // @formatter:on
    @Test
    void unpaidFeeBlocksRiderEveryTime() {
        // given
        var policy = new BlockRiderWithOutstandingDebtPolicy(accountBlocks);

        // when
        policy.handle(new RentalFeeUnpaid(BEN, S_3, pln("43.00"), pln("43.00"), at("10:40")));
        policy.handle(new RentalFeeUnpaid(BEN, S_5, pln("3.00"), pln("46.00"), at("11:00")));

        // then
        assertThat(accountBlocks.requests).containsExactly("block ben", "block ben");
    }

    // @formatter:off
    @DisplayName(
        """
         Payoff E1.1 given Anna's outstanding debt was cleared at 18:00 (Outstanding Debt Cleared),
         when the policy handles it,
         then Accounts is asked to lift Anna's block for outstanding debt
        """
    )
    // @formatter:on
    @Test
    void clearedDebtLiftsDebtBlock() {
        // given
        var policy = new UnblockRiderWhenDebtClearedPolicy(accountBlocks);

        // when
        policy.handle(new OutstandingDebtCleared(new RiderId("anna"), pln("43.00"), at("18:00")));

        // then
        assertThat(accountBlocks.requests).containsExactly("unblock anna");
    }
}
