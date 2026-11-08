package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

/// Why the rental session ended, in Rentals' published terms (Settlement is conformist to Rentals' PL).
public enum EndReason {
    STATION_RETURN, FREE_FLOATING_RETURN, LOCK_FAILURE, MAX_RENTAL_TIME_EXCEEDED
}
