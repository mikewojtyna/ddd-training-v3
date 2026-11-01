package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;

/**
 * Requested length of a rental. A rental may last at most {@link #MAXIMUM}.
 */
@ValueObject
public record RentalDuration(Duration duration) {

    public static final Duration MAXIMUM = Duration.ofHours(11);

    public boolean exceedsMaximum() {
        return duration.compareTo(MAXIMUM) > 0;
    }
}
