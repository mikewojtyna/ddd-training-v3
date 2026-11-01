package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import java.util.List;

/// Result of the "Available Bikes At Station" query: the ids of the bikes that can be rented right now.
public record AvailableBikesAtStation(String stationId, List<String> bikeIds) {

    public AvailableBikesAtStation {
        bikeIds = List.copyOf(bikeIds);
    }
}
