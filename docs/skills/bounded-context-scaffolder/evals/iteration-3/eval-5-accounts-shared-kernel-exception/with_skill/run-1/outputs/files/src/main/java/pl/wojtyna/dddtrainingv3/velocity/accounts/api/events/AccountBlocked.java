package pl.wojtyna.dddtrainingv3.velocity.accounts.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

/// Published language for the canvas outbound event "Account Blocked" (to Rentals). Primitives only.
@ValueObject
public record AccountBlocked(String borrowerId) implements IntegrationEvent {
}
