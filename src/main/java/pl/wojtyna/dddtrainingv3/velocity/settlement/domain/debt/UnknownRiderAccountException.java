package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

/// Accounts doesn't know the rider Settlement wanted to block or unblock.
public class UnknownRiderAccountException extends RuntimeException {

    public UnknownRiderAccountException(String message) {
        super(message);
    }

    public UnknownRiderAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
