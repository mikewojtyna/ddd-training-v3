package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import java.util.Set;

public record AvailableBikesAtStationView(String bikeStationId, Set<String> bikeIds) {

    public AvailableBikesAtStationView {
        bikeIds = Set.copyOf(bikeIds);
    }
}
