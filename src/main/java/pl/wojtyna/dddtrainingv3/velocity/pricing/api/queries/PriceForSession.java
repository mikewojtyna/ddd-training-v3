package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import java.time.Instant;

/// Query *Price for Session* (Settlement, OHS/PL): the facts of a finished rental session.
///
/// `ending` is one of `STATION`, `FREE_FLOATING`, `LOCK_FAILURE`, `MAX_RENTAL_TIME`. The position is required only
/// for `FREE_FLOATING`; for every other ending it may be missing and is ignored.
public record PriceForSession(Instant startedAt, Instant endedAt, String ending, Double latitude, Double longitude) {
}
