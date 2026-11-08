package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.ErrorResponse;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries.RecoveryProcessQuery;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries.RecoveryProcessQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries.RecoveryProcessView;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.PauseRecovery;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.PauseRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.ResumeRecovery;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.ResumeRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.UnknownPauseReason;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryNotPausedException;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryNotRunningException;

import java.time.Clock;

/// Debt Recovery's REST resources for collections agents: *Pause Recovery*, *Resume Recovery* and the *Recovery
/// Process* query. See `docs/bounded-contexts/velocity/integration-contracts.md` (incl. "Additions").
///
/// A pause or resume answers `200` with the recovery process as it is now; a broken rule is `409`
/// (`RecoveryNotRunning`, `RecoveryNotPaused`); a reason outside the list is `400 UnknownPauseReason`; a rider without
/// a recovery process is `404` for the query.
@PrimaryAdapter
@RestController
@RequestMapping("/debt-recovery")
public class DebtRecoveryController {

    public record PauseRequest(String agentId, String reason) {
    }

    public record ResumeRequest(String agentId) {
    }

    private final PauseRecoveryUseCase pauseRecovery;
    private final ResumeRecoveryUseCase resumeRecovery;
    private final RecoveryProcessQueryHandler recoveryProcess;
    private final Clock clock;

    public DebtRecoveryController(PauseRecoveryUseCase pauseRecovery, ResumeRecoveryUseCase resumeRecovery,
                                  RecoveryProcessQueryHandler recoveryProcess, Clock clock) {
        this.pauseRecovery = pauseRecovery;
        this.resumeRecovery = resumeRecovery;
        this.recoveryProcess = recoveryProcess;
        this.clock = clock;
    }

    @GetMapping("/riders/{riderId}/recovery")
    public ResponseEntity<RecoveryProcessView> recovery(@PathVariable("riderId") String riderId) {
        return ResponseEntity.of(recoveryProcess.handle(new RecoveryProcessQuery(riderId)));
    }

    @PostMapping("/riders/{riderId}/recovery/pause")
    public ResponseEntity<RecoveryProcessView> pause(@PathVariable("riderId") String riderId,
                                                     @RequestBody PauseRequest request) {
        pauseRecovery.handle(new PauseRecovery(riderId, request.reason(), request.agentId(), clock.instant()));
        return recovery(riderId);
    }

    @PostMapping("/riders/{riderId}/recovery/resume")
    public ResponseEntity<RecoveryProcessView> resume(@PathVariable("riderId") String riderId,
                                                      @RequestBody ResumeRequest request) {
        resumeRecovery.handle(new ResumeRecovery(riderId, request.agentId(), clock.instant()));
        return recovery(riderId);
    }

    @ExceptionHandler({RecoveryNotRunningException.class, RecoveryNotPausedException.class,
                       OptimisticLockingException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse ruleBroken(RuntimeException exception) {
        var name = exception.getClass().getSimpleName();
        return new ErrorResponse(name.endsWith("Exception") ? name.substring(0, name.length() - "Exception".length()) : name,
                                 exception.getMessage());
    }

    @ExceptionHandler(UnknownPauseReason.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse unknownPauseReason(UnknownPauseReason exception) {
        return new ErrorResponse("UnknownPauseReason", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse malformed(IllegalArgumentException exception) {
        return new ErrorResponse("MalformedRequest", exception.getMessage());
    }
}
