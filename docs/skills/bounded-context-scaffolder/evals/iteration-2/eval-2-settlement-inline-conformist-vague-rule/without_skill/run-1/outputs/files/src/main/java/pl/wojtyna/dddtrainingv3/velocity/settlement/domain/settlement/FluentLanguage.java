package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

public interface FluentLanguage {

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }
}
