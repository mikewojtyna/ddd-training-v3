package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

/// A session started before the first tariff of the schedule became valid can't be priced.
public class NoTariffValidException extends RuntimeException {

    public NoTariffValidException(String message) {
        super(message);
    }

    public NoTariffValidException(String message, Throwable cause) {
        super(message, cause);
    }
}
