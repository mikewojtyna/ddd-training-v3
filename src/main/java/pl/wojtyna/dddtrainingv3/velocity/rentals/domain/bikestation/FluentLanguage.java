package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public interface FluentLanguage {

    @FunctionalInterface
    interface By<S, R> {

        R by(S s);
    }

    @FunctionalInterface
    interface ForDuration<S, R> {

        R forDuration(S s);
    }

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }
}
