package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

/// Rule: two tariffs can't share the same valid from (DE-96).
public class DuplicateValidFromException extends RuntimeException {

    public DuplicateValidFromException(String message) {
        super(message);
    }

    public DuplicateValidFromException(String message, Throwable cause) {
        super(message, cause);
    }
}
