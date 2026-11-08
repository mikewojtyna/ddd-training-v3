package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

/// Rule: a tariff's valid from must lie in the future when it is published (DE-96).
public class ValidFromNotInFutureException extends RuntimeException {

    public ValidFromNotInFutureException(String message) {
        super(message);
    }

    public ValidFromNotInFutureException(String message, Throwable cause) {
        super(message, cause);
    }
}
