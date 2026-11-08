package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.Factory;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import java.time.Instant;

/// Puts new bikes into service. Creation has rules of its own that no single bike can check: a bike joins the fleet
/// only once (needs the register of all bikes) and only at a station Rentals knows (reference data).
@Factory
public class Fleet {

    public record NewBike(Bike bike, DomainEvents events) {
    }

    private final FleetRegister register;
    private final Stations stations;

    public Fleet(FleetRegister register, Stations stations) {
        this.register = register;
        this.stations = stations;
    }

    /// @throws UnknownStation     if `stationId` is not a station (DE-18)
    /// @throws BikeAlreadyInFleet if the bike already joined the fleet
    public NewBike add(BikeId bikeId, StationId stationId, Instant now) {
        var station = stations.named(stationId);
        if (register.contains(bikeId)) {
            throw new BikeAlreadyInFleet(bikeId.value() + " is already part of the fleet");
        }
        var bike = Bike.availableAt(bikeId, station);
        return new NewBike(bike, DomainEvents.of(new BikeEvent.BikeAddedToFleet(bikeId, station.id(), station.position(), now)));
    }
}
