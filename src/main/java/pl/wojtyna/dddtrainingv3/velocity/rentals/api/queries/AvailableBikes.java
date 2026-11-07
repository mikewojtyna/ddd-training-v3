package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import java.util.List;

/// Result of `AvailableBikesAtStation`: the ids of the bikes a borrower can reserve or rent at the station right now.
public record AvailableBikes(String stationId, List<String> bikeIds) {
}
