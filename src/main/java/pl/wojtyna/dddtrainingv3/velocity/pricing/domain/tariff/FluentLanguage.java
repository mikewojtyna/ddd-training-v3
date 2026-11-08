package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

/// Steps of the chained commands of the tariff schedule, e.g. `schedule.publish(tariff).by(paula).at(now)`.
public interface FluentLanguage {

    @FunctionalInterface
    interface By<S, R> {

        R by(S s);
    }

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }
}
