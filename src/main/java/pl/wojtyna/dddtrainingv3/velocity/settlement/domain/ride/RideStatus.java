package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

/// Where a ride stands: waiting for a (further) charge attempt, settled, or unpaid after 3 failed attempts.
public enum RideStatus {
    PENDING, SETTLED, UNPAID
}
