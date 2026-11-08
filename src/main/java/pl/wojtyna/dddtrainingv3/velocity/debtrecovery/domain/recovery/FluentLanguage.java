package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

/// Steps of Debt Recovery's chained commands, e.g. `process.pause(reason).by(agent).at(now)`.
public interface FluentLanguage {

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }

    @FunctionalInterface
    interface By<S, R> {

        R by(S s);
    }

    @FunctionalInterface
    interface As<S, R> {

        R as(S s);
    }
}
