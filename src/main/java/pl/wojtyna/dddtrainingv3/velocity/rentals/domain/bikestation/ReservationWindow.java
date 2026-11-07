package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.time.Instant;

/// How long a reservation holds a bike: 15 minutes from the moment of reserving, including the end time
/// (product owner, 2026-11-07). Once it has passed, the reservation stops counting (plan D9).
@ValueObject
public record ReservationWindow(Instant start) {

    private static final Duration LENGTH = Duration.ofMinutes(15);

    public Instant end() {
        return start.plus(LENGTH);
    }

    public boolean isOpenAt(Instant now) {
        return !now.isAfter(end());
    }
}
