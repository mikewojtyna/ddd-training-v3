package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// A point on the map (WGS84 degrees). Distances are great-circle distances (haversine) rounded to centimetres, so
/// "exactly 50 m" compares as exactly 50 m (spec `rider-reserves-bike` Q1).
@ValueObject
public record Position(double latitude, double longitude) {

    public static final double EARTH_RADIUS_METRES = 6_371_008.8;

    public Position {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Not a position: " + latitude + ", " + longitude);
        }
    }

    public BigDecimal metresTo(Position other) {
        var lat1 = Math.toRadians(latitude);
        var lat2 = Math.toRadians(other.latitude);
        var dLat = lat2 - lat1;
        var dLon = Math.toRadians(other.longitude - longitude);
        var a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        var metres = 2 * EARTH_RADIUS_METRES * Math.asin(Math.min(1.0, Math.sqrt(a)));
        return BigDecimal.valueOf(metres).setScale(2, RoundingMode.HALF_UP);
    }

    /// The position `metres` due north of this one (same longitude); handy to place a rider at a known distance.
    public Position northBy(double metres) {
        return new Position(latitude + Math.toDegrees(metres / EARTH_RADIUS_METRES), longitude);
    }
}
