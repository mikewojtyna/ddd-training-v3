package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.PauseRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.ResumeRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.PauseReason;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessId;

/**
 * Back-office endpoints for pausing and resuming recovery (disputes, payment plan negotiation, regulatory holds).
 */
@PrimaryAdapter
@RestController
@RequestMapping("/velocity/recovery-processes/{processId}")
public class RecoveryProcessRestApi {

    private final PauseRecoveryUseCase pauseRecoveryUseCase;
    private final ResumeRecoveryUseCase resumeRecoveryUseCase;

    public RecoveryProcessRestApi(PauseRecoveryUseCase pauseRecoveryUseCase,
                                  ResumeRecoveryUseCase resumeRecoveryUseCase) {
        this.pauseRecoveryUseCase = pauseRecoveryUseCase;
        this.resumeRecoveryUseCase = resumeRecoveryUseCase;
    }

    @PostMapping("/pause")
    public void pause(@PathVariable("processId") String processId, @RequestBody PauseRequest request) {
        pauseRecoveryUseCase.pause(new RecoveryProcessId(processId), PauseReason.valueOf(request.reason()));
    }

    @PostMapping("/resume")
    public void resume(@PathVariable("processId") String processId) {
        resumeRecoveryUseCase.resume(new RecoveryProcessId(processId));
    }

    public record PauseRequest(String reason) {
    }
}
