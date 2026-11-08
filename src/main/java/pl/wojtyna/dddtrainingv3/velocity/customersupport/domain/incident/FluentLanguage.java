package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// Steps of the incident's chained commands, e.g. `incident.escalateBy(zoe).to(BILLING).at(now)`.
public interface FluentLanguage {

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }

    @FunctionalInterface
    interface To<S, R> {

        R to(S s);
    }

    @FunctionalInterface
    interface With<S, R> {

        R with(S s);
    }

    @FunctionalInterface
    interface Because<S, R> {

        R because(S s);
    }

    @FunctionalInterface
    interface Within<S, R> {

        R within(S s);
    }
}
