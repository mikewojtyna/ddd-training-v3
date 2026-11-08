package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.function.Supplier;

/// Event-driven use cases run on listener threads, possibly at the same time for one bike (e.g. a fleet issue and a
/// lock failure): a lost optimistic-locking race is retried with a fresh load.
final class Retrying {

    private static final int ATTEMPTS = 3;

    private Retrying() {
    }

    static <T> T onConflict(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return work.get();
            } catch (OptimisticLockingException conflict) {
                if (attempt == ATTEMPTS) {
                    throw conflict;
                }
            }
        }
    }
}
