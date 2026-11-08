package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Steps of Rentals' chained commands, e.g. `bike.reserveForRide(anna).standingAt(position).given(standing).at(now)`.
public interface FluentLanguage {

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }

    @FunctionalInterface
    interface StandingAt<S, R> {

        R standingAt(S s);
    }

    @FunctionalInterface
    interface Given<S, R> {

        R given(S s);
    }

    @FunctionalInterface
    interface Using<S, R> {

        R using(S s);
    }

    @FunctionalInterface
    interface By<S, R> {

        R by(S s);
    }

    @FunctionalInterface
    interface Reporting<S, R> {

        R reporting(S s);
    }
}
