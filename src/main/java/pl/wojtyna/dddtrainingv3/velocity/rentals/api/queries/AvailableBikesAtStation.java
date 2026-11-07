package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

/// Canvas inbound query *Available bikes at station* (Rider app). Reserved and withdrawn bikes aren't listed (rule R4).
public record AvailableBikesAtStation(String stationId) {
}
