package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeOutcome;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebt;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason.DECLINED;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln.pln;

@DisplayName("Settlement's in-memory repositories")
class InMemorySettlementRepositoriesTest {

    private static final Instant NOW = Instant.parse("2026-11-09T10:30:00Z");
    private static final RiderId ANNA = new RiderId("anna");
    private static final SessionId S_1 = new SessionId("S-1");

    @Test
    @DisplayName("a ride modified by two copies loaded at the same version is saved only once")
    void rideSaveIsOptimisticallyLocked() {
        // given
        var repository = new InMemoryRideSettlementRepository();
        repository.save(RideSettlement.of(S_1).riddenBy(ANNA).pricedAt(pln("5.00")).at(NOW).ride());
        var first = repository.load(S_1).orElseThrow();
        var second = repository.load(S_1).orElseThrow();
        first.recordCharge(ChargeOutcome.success()).at(NOW);
        second.recordCharge(ChargeOutcome.failure(DECLINED)).at(NOW);
        repository.save(first);

        // when
        var thrown = catchThrowable(() -> repository.save(second));

        // then
        assertThat(thrown).isInstanceOf(OptimisticLockingException.class);
        assertThat(repository.rideSettlementOf("S-1")).hasValueSatisfying(view -> assertThat(view.status()).isEqualTo("SETTLED"));
    }

    @Test
    @DisplayName("a second new ride for the same session is refused (DE-95)")
    void secondNewRideForSameSessionIsRefused() {
        // given
        var repository = new InMemoryRideSettlementRepository();
        repository.save(RideSettlement.of(S_1).riddenBy(ANNA).pricedAt(pln("5.00")).at(NOW).ride());

        // when
        var thrown = catchThrowable(() -> repository.save(RideSettlement.of(S_1).riddenBy(ANNA).pricedAt(pln("5.00")).at(NOW).ride()));

        // then
        assertThat(thrown).isInstanceOf(OptimisticLockingException.class);
    }

    @Test
    @DisplayName("a debt modified by two copies loaded at the same version is saved only once")
    void debtSaveIsOptimisticallyLocked() {
        // given
        var repository = new InMemoryRiderDebtRepository();
        repository.save(RiderDebt.none(ANNA));
        var first = repository.load(ANNA).orElseThrow();
        var second = repository.load(ANNA).orElseThrow();
        first.addUnpaidFee(new SessionId("S-3")).of(pln("43.00")).at(NOW);
        second.addUnpaidFee(new SessionId("S-5")).of(pln("3.00")).at(NOW);
        repository.save(first);

        // when
        var thrown = catchThrowable(() -> repository.save(second));

        // then
        assertThat(thrown).isInstanceOf(OptimisticLockingException.class);
        assertThat(repository.balanceOf("anna")).hasValueSatisfying(balance -> assertThat(balance.outstandingDebt()).isEqualTo("43.00"));
    }
}
