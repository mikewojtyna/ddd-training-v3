package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening;

/// Steps of chained commands, e.g. `screening.reserve(seat).by(customer)`.
public interface FluentLanguage {

    @FunctionalInterface
    interface By<S, R> {

        R by(S s);
    }
}
