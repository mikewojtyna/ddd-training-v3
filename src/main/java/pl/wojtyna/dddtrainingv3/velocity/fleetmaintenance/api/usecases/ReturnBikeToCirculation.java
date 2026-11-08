package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import java.math.BigDecimal;
import java.time.Instant;

/// Canvas command *Return Bike to Circulation* (technician app): at a station (`stationId`) or at a position
/// (`latitude`, `longitude`). It means the bike is fixed (DE-60, DE-91).
public record ReturnBikeToCirculation(String bikeId, String technicianId, String stationId, BigDecimal latitude,
                                      BigDecimal longitude, Instant returnedAt) {
}
