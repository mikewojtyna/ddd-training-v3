package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import java.math.BigDecimal;

/// `availability`: `AVAILABLE`|`WITHDRAWN`; `hold`: `FREE`|`RESERVED`|`UNLOCKED`; position null when unknown.
public record BikeAvailability(String bikeId, String availability, String hold, String holderType, String holderId, BigDecimal latitude, BigDecimal longitude, String stationId) {
}
