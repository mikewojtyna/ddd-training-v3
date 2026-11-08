package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.AgentId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.PauseReason;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcess;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.Pln.pln;

@DisplayName("In-memory recovery processes")
class InMemoryRecoveryProcessesTest {

    private static final RiderId ANNA = new RiderId("anna");
    private static final Instant START = Instant.parse("2026-11-08T10:50:00Z");

    @Test
    @DisplayName("a stale copy of a recovery process is not saved (optimistic locking)")
    void staleCopyIsRejected() {
        // given
        var repository = new InMemoryRecoveryProcesses();
        var process = RecoveryProcess.none(ANNA);
        process.unpaidFee(pln("5.00")).at(START);
        repository.save(process);
        var first = repository.load(ANNA).orElseThrow();
        var second = repository.load(ANNA).orElseThrow();
        first.pause(PauseReason.DISPUTE).by(new AgentId("kate")).at(START.plusSeconds(60));
        second.unpaidFee(pln("8.00")).at(START.plusSeconds(120));
        repository.save(first);

        // when
        var exception = catchThrowable(() -> repository.save(second));

        // then
        assertThat(exception).isInstanceOf(OptimisticLockingException.class);
        assertThat(repository.recoveryOf("anna")).hasValueSatisfying(view -> {
            assertThat(view.status()).isEqualTo("PAUSED");
            assertThat(view.pauseReason()).isEqualTo("DISPUTE");
            assertThat(view.outstandingDebt()).isEqualTo("5.00");
            assertThat(view.nextAttemptDueAt()).isNull();
        });
    }

    @Test
    @DisplayName("only running recovery processes are candidates for a collection attempt")
    void loadsOnlyRunningProcesses() {
        // given
        var repository = new InMemoryRecoveryProcesses();
        var running = RecoveryProcess.none(ANNA);
        running.unpaidFee(pln("5.00")).at(START);
        repository.save(running);
        var paused = RecoveryProcess.none(new RiderId("ben"));
        paused.unpaidFee(pln("3.00")).at(START);
        paused.pause(PauseReason.PAYMENT_PLAN).by(new AgentId("kate")).at(START);
        repository.save(paused);

        // when
        var candidates = repository.loadRunning();

        // then
        assertThat(candidates).extracting(RecoveryProcess::rider).containsExactly(ANNA);
    }
}
