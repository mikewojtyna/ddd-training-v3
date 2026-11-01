package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;

import java.time.Clock;

/**
 * Retries every settlement whose next charge attempt is due. Triggered periodically.
 */
@PrimaryPort
public class RetryDueChargeAttemptsUseCase {

    private final SettlementRepository settlementRepository;
    private final AttemptChargeUseCase attemptChargeUseCase;
    private final Clock clock;

    public RetryDueChargeAttemptsUseCase(SettlementRepository settlementRepository,
                                         AttemptChargeUseCase attemptChargeUseCase, Clock clock) {
        this.settlementRepository = settlementRepository;
        this.attemptChargeUseCase = attemptChargeUseCase;
        this.clock = clock;
    }

    public void retryDue() {
        settlementRepository.findDueForChargeAttempt(clock.instant()).forEach(attemptChargeUseCase::attempt);
    }
}
