package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

/// Steps of the curried commands of `RiderPaymentMethods`, e.g. `verify(card).as(id).at(now).using(processor)`.
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
    interface Using<S, R> {

        R using(S s);
    }

    @FunctionalInterface
    interface ForReference<S, R> {

        R forReference(S s);
    }

    @FunctionalInterface
    interface Because<S, R> {

        R because(S s);
    }
}
