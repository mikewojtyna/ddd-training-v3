package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface BikeRentalEvent extends DomainEvent {

    record BikePlacedInStation(BikeStationId stationId, BikeId bikeId) implements BikeRentalEvent {
    }

    record BikeRented(BikeStationId stationId, BikeId bikeId, Borrower by, RentalDuration forDuration)
        implements BikeRentalEvent {
    }

    record BikeReturned(BikeStationId stationId, BikeId bikeId, Borrower by) implements BikeRentalEvent {
    }
}
