package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;

public sealed interface BikeRentalEvent extends DomainEvent {

    record BikeRented(BikeId bike, Borrower by, RentalDuration forDuration) implements BikeRentalEvent {
    }

    record BikeReturned() implements BikeRentalEvent {
    }

    record BikeReserved(BikeStationId station, BikeId bike, Borrower by, Instant reservedAt, Instant reservedUntil)
        implements BikeRentalEvent {
    }
}
