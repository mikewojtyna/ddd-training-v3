package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

public interface FluentLanguage {

    @FunctionalInterface
    interface WithReturnCharges<S, R> {

        R withReturnCharges(S s);
    }
}
