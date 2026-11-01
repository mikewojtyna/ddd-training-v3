package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

public class ChargeAttemptNotDueException extends RuntimeException {

    public ChargeAttemptNotDueException(String message) {
        super(message);
    }
}
