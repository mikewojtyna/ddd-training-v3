package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

public interface FluentLanguage {

    @FunctionalInterface
    interface Because<S, R> {

        R because(S s);
    }
}
