package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

/**
 * Thrown when a command reaches a recovery process that has already succeeded or been exhausted.
 */
public class RecoveryProcessClosedException extends RuntimeException {

    public RecoveryProcessClosedException(String message) {
        super(message);
    }

    public RecoveryProcessClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
