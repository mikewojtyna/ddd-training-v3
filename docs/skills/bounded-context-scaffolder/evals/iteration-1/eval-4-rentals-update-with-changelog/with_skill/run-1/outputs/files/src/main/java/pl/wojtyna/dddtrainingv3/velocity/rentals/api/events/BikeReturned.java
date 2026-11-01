package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

/// Canvas outbound event "Bike Returned", consumed by Pricing. Published language: primitives only.
@ValueObject
public record BikeReturned(String bikeStationId, String bikeId) implements IntegrationEvent {
}
