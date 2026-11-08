package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone;

import org.jmolecules.ddd.annotation.ValueObject;

/// Where a session ended, in decimal degrees.
@ValueObject
public record Position(double latitude, double longitude) {

    public Position {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Not a position: " + latitude + ", " + longitude);
        }
    }
}
