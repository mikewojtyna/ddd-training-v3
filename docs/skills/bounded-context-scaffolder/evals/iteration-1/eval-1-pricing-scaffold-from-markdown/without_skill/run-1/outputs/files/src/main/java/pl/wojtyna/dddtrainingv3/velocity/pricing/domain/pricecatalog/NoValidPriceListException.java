package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

public class NoValidPriceListException extends RuntimeException {

    public NoValidPriceListException(String message) {
        super(message);
    }
}
