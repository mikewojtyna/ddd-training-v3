package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

public class PriceListAlreadyPublishedException extends RuntimeException {

    public PriceListAlreadyPublishedException(String message) {
        super(message);
    }
}
