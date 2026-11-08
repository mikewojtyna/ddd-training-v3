package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedCharges;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedChargesUseCase;

import java.time.Clock;

/// Finance's clock (DE-35): regularly retries the failed charges that are due. A retry is due 5 minutes after the
/// failed attempt; the job's interval only decides how late after that it happens. Runs only when scheduling is on
/// (`velocity.scheduling.enabled`, off in tests, which call the use case directly).
@PrimaryAdapter
public class FailedChargeRetryJob {

    private final RetryFailedChargesUseCase retryFailedCharges;
    private final Clock clock;

    public FailedChargeRetryJob(RetryFailedChargesUseCase retryFailedCharges, Clock clock) {
        this.retryFailedCharges = retryFailedCharges;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${velocity.settlement.charge-retry-delay-ms:30000}")
    public void retryFailedCharges() {
        retryFailedCharges.handle(new RetryFailedCharges(clock.instant()));
    }
}
