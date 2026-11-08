package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

/// Rule: fees can't be negative (DE-96).
public class NegativeFeeException extends RuntimeException {

    public NegativeFeeException(String message) {
        super(message);
    }

    public NegativeFeeException(String message, Throwable cause) {
        super(message, cause);
    }
}
