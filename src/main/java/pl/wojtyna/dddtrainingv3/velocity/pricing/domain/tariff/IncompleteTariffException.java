package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

/// Rule: a tariff states every price (vision, DE-30).
public class IncompleteTariffException extends RuntimeException {

    public IncompleteTariffException(String message) {
        super(message);
    }

    public IncompleteTariffException(String message, Throwable cause) {
        super(message, cause);
    }
}
