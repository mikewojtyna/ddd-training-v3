package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Steps of the bike's chained commands, e.g. `bike.reportFault(report).at(now)`,
/// `bike.reserveFor(tom).through(serviceReservations)`.
public interface FluentLanguage {

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }

    @FunctionalInterface
    interface Through<S, R> {

        R through(S s);
    }
}
