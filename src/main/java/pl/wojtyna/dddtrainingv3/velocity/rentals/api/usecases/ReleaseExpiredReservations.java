package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import java.time.Instant;

/// Fleet operations' clock: release every reservation whose period is over at `now`.
public record ReleaseExpiredReservations(Instant now) {
}
