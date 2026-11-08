package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

/// Steps of the curried commands of `Account` and `RiderRegistration`, e.g.
/// `account.providePersonalData(data).on(today).at(now)`.
public interface FluentLanguage {

    @FunctionalInterface
    interface As<S, R> {

        R as(S s);
    }

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }

    @FunctionalInterface
    interface On<S, R> {

        R on(S s);
    }

    @FunctionalInterface
    interface Using<S, R> {

        R using(S s);
    }
}
