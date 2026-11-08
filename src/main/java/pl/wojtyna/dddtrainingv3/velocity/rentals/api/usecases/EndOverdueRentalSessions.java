package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import java.time.Instant;

/// Fleet operations' clock: end every session running longer than the maximum rental time at `now`.
public record EndOverdueRentalSessions(Instant now) {
}
