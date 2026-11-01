package pl.wojtyna.dddtrainingv3.velocity.accounts.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

/**
 * Published event: the account was blocked, so the rider must not rent bikes. Meant for Rentals.
 * <p>
 * Carries primitives only, like other published events. The internal
 * {@code domain.account.AccountEvent.AccountBlocked} is a different class.
 */
@ValueObject
public record AccountBlocked(String borrowerId) implements IntegrationEvent {
}
