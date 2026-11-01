package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

/// Published language for the canvas outbound event "Bike Returned" (consumer: Pricing).
/// Carries strings only. Rental start and end times are not part of it yet (see the plan, Q6).
@ValueObject
public record BikeReturned(String stationId, String bikeId, String borrowerId) implements IntegrationEvent {
}
