package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

/**
 * Published by Rentals when a rented bike is returned to a bike station. Consumed by Pricing.
 */
@ValueObject
public record BikeReturned(String stationId, String bikeId, String borrowerId) implements IntegrationEvent {
}
