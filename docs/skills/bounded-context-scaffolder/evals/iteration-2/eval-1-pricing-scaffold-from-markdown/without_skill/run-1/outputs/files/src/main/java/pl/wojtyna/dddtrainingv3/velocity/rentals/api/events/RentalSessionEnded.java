package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/**
 * Rental Session Ended: published language of Rentals. Nothing in Rentals publishes it yet.
 */
@ValueObject
public record RentalSessionEnded(String rentalSessionId, String riderId, Instant startedAt, Instant endedAt)
    implements IntegrationEvent {
}
