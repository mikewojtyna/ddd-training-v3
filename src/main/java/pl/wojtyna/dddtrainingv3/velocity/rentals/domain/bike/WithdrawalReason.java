package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

/// Why a bike was withdrawn from riders. Rentals never says *broken*: that is its clients' word.
public enum WithdrawalReason {
    FAULT_REPORTED_AT_RETURN,
    LOCK_FAILED,
    FAULT_SIGNALLED,
    NOT_RETURNED,
    REQUESTED
}
