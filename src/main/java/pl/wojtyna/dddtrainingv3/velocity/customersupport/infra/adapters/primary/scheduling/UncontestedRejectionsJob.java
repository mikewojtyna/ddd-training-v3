package pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.primary.scheduling;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases.ResolveUncontestedRejections;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases.ResolveUncontestedRejectionsUseCase;

import java.time.Clock;

/// The support clock (DE-87): regularly resolves rejected incidents whose reopening window has closed. Runs only when
/// scheduling is on (`velocity.scheduling.enabled`, off in tests, which call the use case directly).
@PrimaryAdapter
public class UncontestedRejectionsJob {

    private final ResolveUncontestedRejectionsUseCase resolveUncontestedRejections;
    private final Clock clock;

    public UncontestedRejectionsJob(ResolveUncontestedRejectionsUseCase resolveUncontestedRejections, Clock clock) {
        this.resolveUncontestedRejections = resolveUncontestedRejections;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${velocity.customer-support.uncontested-rejections-delay-ms:60000}")
    public void resolveUncontestedRejections() {
        resolveUncontestedRejections.handle(new ResolveUncontestedRejections(clock.instant()));
    }
}
