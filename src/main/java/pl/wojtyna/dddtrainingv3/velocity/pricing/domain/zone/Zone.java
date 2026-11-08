package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone;

import org.jmolecules.ddd.annotation.ValueObject;

/// What the zone map says about a position: inside a drop zone, or in the unauthorized zone (outside every drop
/// zone).
@ValueObject
public enum Zone {
    DROP_ZONE,
    UNAUTHORIZED_ZONE
}
