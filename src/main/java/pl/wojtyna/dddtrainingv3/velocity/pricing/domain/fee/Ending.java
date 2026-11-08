package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee;

import org.jmolecules.ddd.annotation.ValueObject;

/// How a session ended.
@ValueObject
public enum Ending {
    /// returned at a station
    STATION,
    /// ended by the rider at their current position, outside a station
    FREE_FLOATING,
    /// the lock didn't close when the rider tried to end the session
    LOCK_FAILURE,
    /// ended automatically at the maximum rental time
    MAX_RENTAL_TIME
}
