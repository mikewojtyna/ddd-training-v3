package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

/// Rule: a tariff's id is unique (spec `pricing-manager-publishes-tariff`, Data).
public class DuplicateTariffIdException extends RuntimeException {

    public DuplicateTariffIdException(String message) {
        super(message);
    }

    public DuplicateTariffIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
