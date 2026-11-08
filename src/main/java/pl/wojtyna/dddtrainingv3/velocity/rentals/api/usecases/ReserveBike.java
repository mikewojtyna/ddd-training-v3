package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import java.math.BigDecimal;

/// Reserve a bike: a rider (`RIDER`, with the rider's position) for a ride, a technician (`TECHNICIAN`, no position) for service.
public record ReserveBike(String bikeId, String holderType, String holderId, BigDecimal latitude, BigDecimal longitude) {
}
