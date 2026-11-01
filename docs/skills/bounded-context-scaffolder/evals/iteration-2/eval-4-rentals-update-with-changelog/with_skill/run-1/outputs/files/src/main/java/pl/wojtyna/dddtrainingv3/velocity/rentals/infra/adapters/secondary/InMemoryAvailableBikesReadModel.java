package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStation;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesReadModel;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeRentalEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// Projection of `BikePlacedInStation`, `BikeRented` and `BikeReturned` into the bikes available per station.
@SecondaryAdapter
public class InMemoryAvailableBikesReadModel implements AvailableBikesReadModel {

    private final Map<String, Set<String>> availableBikesByStation = new ConcurrentHashMap<>();

    @EventListener
    public void on(BikeRentalEvent event) {
        switch (event) {
            case BikeRentalEvent.BikePlacedInStation placed -> bikesAt(placed.stationId().value())
                .add(placed.bikeId().value());
            case BikeRentalEvent.BikeRented rented -> bikesAt(rented.stationId().value())
                .remove(rented.bikeId().value());
            case BikeRentalEvent.BikeReturned returned -> bikesAt(returned.stationId().value())
                .add(returned.bikeId().value());
        }
    }

    @Override
    public AvailableBikesAtStation availableAt(String stationId) {
        var bikeIds = availableBikesByStation.getOrDefault(stationId, Set.of()).stream().sorted().toList();
        return new AvailableBikesAtStation(stationId, bikeIds);
    }

    private Set<String> bikesAt(String stationId) {
        return availableBikesByStation.computeIfAbsent(stationId, _ -> ConcurrentHashMap.newKeySet());
    }
}
