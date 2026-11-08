package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import java.time.Instant;

/// *Attempt Due Collections*: Collections' clock asks for every collection attempt due at `now`.
public record AttemptDueCollections(Instant now) {
}
