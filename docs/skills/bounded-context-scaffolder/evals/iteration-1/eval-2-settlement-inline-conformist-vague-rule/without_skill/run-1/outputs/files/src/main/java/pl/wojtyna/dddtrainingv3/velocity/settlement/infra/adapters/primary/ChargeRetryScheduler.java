package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryDueChargeAttemptsUseCase;

@PrimaryAdapter
public class ChargeRetryScheduler {

    private final RetryDueChargeAttemptsUseCase retryDueChargeAttemptsUseCase;

    public ChargeRetryScheduler(RetryDueChargeAttemptsUseCase retryDueChargeAttemptsUseCase) {
        this.retryDueChargeAttemptsUseCase = retryDueChargeAttemptsUseCase;
    }

    @Scheduled(fixedDelayString = "${velocity.settlement.retry-poll-interval:PT1M}")
    public void retryDueChargeAttempts() {
        retryDueChargeAttemptsUseCase.retryDue();
    }
}
