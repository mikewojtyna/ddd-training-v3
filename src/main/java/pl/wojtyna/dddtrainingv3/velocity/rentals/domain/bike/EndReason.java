package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Why a rental session ended.
public enum EndReason {
    STATION_RETURN,
    FREE_FLOATING_RETURN,
    LOCK_FAILURE,
    MAX_RENTAL_TIME_EXCEEDED
}
