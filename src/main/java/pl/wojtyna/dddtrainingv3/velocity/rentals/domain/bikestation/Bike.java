package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Bike(BikeId id) {
}
