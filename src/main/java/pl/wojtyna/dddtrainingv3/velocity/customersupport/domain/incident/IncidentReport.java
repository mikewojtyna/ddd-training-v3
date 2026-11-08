package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Optional;

/// What the rider reported: category, their own words and, optionally, the bike.
@ValueObject
public record IncidentReport(RiderId rider, Category category, Description description, Optional<BikeId> bike) {
}
