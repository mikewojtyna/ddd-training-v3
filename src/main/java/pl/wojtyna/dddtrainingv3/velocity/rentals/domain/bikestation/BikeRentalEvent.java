package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface BikeRentalEvent extends DomainEvent {

    record BikeRented(BikeId bike, Borrower by, RentalDuration forDuration) implements BikeRentalEvent {
    }

    record BikeReturned() implements BikeRentalEvent {
    }
}
