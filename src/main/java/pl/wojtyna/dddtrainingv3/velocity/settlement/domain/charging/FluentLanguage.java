package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging;

/// Steps of Settlement's curried commands, e.g. `RideSettlement.of(session).riddenBy(rider).pricedAt(fee).at(now)`.
public interface FluentLanguage {

    @FunctionalInterface
    interface RiddenBy<S, R> {

        R riddenBy(S s);
    }

    @FunctionalInterface
    interface PricedAt<S, R> {

        R pricedAt(S s);
    }

    @FunctionalInterface
    interface Of<S, R> {

        R of(S s);
    }

    @FunctionalInterface
    interface As<S, R> {

        R as(S s);
    }

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }
}
